package com.studia.aplikacja_trenerska.trainer.service;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;

import com.studia.aplikacja_trenerska.progress.dto.BodyMetricResponse;
import com.studia.aplikacja_trenerska.progress.dto.ExerciseResultResponse;
import com.studia.aplikacja_trenerska.progress.service.ProgressService;
import com.studia.aplikacja_trenerska.trainer.dto.TrainerDtos.*;
import com.studia.aplikacja_trenerska.trainer.entity.*;
import com.studia.aplikacja_trenerska.trainer.repository.*;
import com.studia.aplikacja_trenerska.user.entity.User;
import com.studia.aplikacja_trenerska.user.entity.UserRole;
import com.studia.aplikacja_trenerska.user.repository.UserRepository;
import com.studia.aplikacja_trenerska.workout.entity.*;
import com.studia.aplikacja_trenerska.workout.repository.WorkoutPlanRepository;

@Service
@Transactional(readOnly = true)
public class TrainerService {
    private final TrainingGroupRepository groups;
    private final GroupMemberRepository members;
    private final PlanAssignmentRepository assignments;
    private final UserRepository users;
    private final WorkoutPlanRepository plans;
    private final ProgressService progress;
    @PersistenceContext private EntityManager entityManager;

    public TrainerService(TrainingGroupRepository groups, GroupMemberRepository members,
            PlanAssignmentRepository assignments, UserRepository users, WorkoutPlanRepository plans,
            ProgressService progress) {
        this.groups = groups;
        this.members = members;
        this.assignments = assignments;
        this.users = users;
        this.plans = plans;
        this.progress = progress;
    }

    public List<GroupResponse> list(String email) {
        return groups.findAllByTrainerIdOrderByCreatedAtDescIdDesc(trainer(email, false).getId())
                .stream().map(this::groupResponse).toList();
    }

    public GroupResponse get(String email, Long groupId) {
        return groupResponse(ownedGroup(trainer(email, false), groupId, false));
    }

    @Transactional
    public GroupResponse create(String email, GroupRequest request) {
        User trainer = trainer(email, true);
        if (groups.countByTrainerId(trainer.getId()) >= 50) throw conflict("Możesz utworzyć maksymalnie 50 grup");
        TrainingGroup group = new TrainingGroup();
        group.setTrainerId(trainer.getId());
        group.setName(request.name().strip());
        return groupResponse(groups.saveAndFlush(group));
    }

    @Transactional
    public void delete(String email, Long groupId) {
        groups.delete(ownedGroup(trainer(email, true), groupId, true));
        groups.flush();
    }

    @Transactional
    public GroupResponse invite(String email, Long groupId, InviteRequest request) {
        User trainer = trainer(email, false);
        ownedGroup(trainer, groupId, false);
        Long inviterId = trainer.getId();
        User trainee = users.findByEmailIgnoreCase(request.email().strip()).filter(User::isEnabled)
                .filter(user -> user.getRole() == UserRole.USER && !user.getId().equals(inviterId))
                .orElseThrow(() -> missing("Nie znaleziono aktywnego konta podopiecznego"));
        lockUsers(Stream.of(trainer.getId(), trainee.getId()).toList());
        // Refresh validates current role/enabled flags after acquiring the user locks.
        trainer = trainer(email, false);
        trainee = users.findById(trainee.getId()).filter(User::isEnabled).filter(user -> user.getRole() == UserRole.USER)
                .orElseThrow(() -> missing("Nie znaleziono aktywnego konta podopiecznego"));
        TrainingGroup group = ownedGroup(trainer, groupId, true);
        if (members.findById(new GroupMemberId(groupId, trainee.getId())).isPresent()) {
            return groupResponse(group); // Repeated requests do not reset an accepted relationship.
        }
        if (members.countByGroupId(groupId) >= 100) throw conflict("Grupa może mieć maksymalnie 100 podopiecznych");
        GroupMember member = new GroupMember();
        member.setGroupId(groupId);
        member.setUserId(trainee.getId());
        members.saveAndFlush(member);
        return groupResponse(group);
    }

    @Transactional
    public void removeMember(String email, Long groupId, Long userId) {
        ownedGroup(trainer(email, true), groupId, true);
        members.delete(lockedMember(groupId, userId));
        members.flush();
    }

    public List<InvitationResponse> invitations(String email) {
        User user = currentUser(email, false);
        return members.findAllByUserIdOrderByInvitedAtDescGroupIdDesc(user.getId()).stream()
                .filter(member -> activeTrainer(groups.findById(member.getGroupId()).orElseThrow().getTrainerId()))
                .map(member -> {
                    TrainingGroup group = groups.findById(member.getGroupId()).orElseThrow();
                    User trainer = users.findById(group.getTrainerId()).orElseThrow();
                    return new InvitationResponse(group.getId(), group.getName(), trainer.getDisplayName(),
                            member.getStatus(), member.getInvitedAt(), member.getAcceptedAt());
                }).toList();
    }

    @Transactional
    public void accept(String email, Long groupId) {
        // This mutation changes no parent FK. Lock only the existing membership so
        // removal/account deletion serializes without taking recipient user locks backwards.
        User user = currentUser(email, false);
        GroupMember member = lockedMember(groupId, user.getId());
        TrainingGroup group = groups.findById(groupId).orElseThrow(() -> missing("Nie znaleziono zaproszenia"));
        if (!activeTrainer(group.getTrainerId())) throw missing("Nie znaleziono aktywnego trenera");
        if (!member.isAccepted()) member.accept();
        members.saveAndFlush(member);
    }

    @Transactional
    public void leave(String email, Long groupId) {
        User user = currentUser(email, false);
        members.delete(lockedMember(groupId, user.getId()));
        members.flush();
    }

    @Transactional
    public List<AssignmentResponse> assign(String email, Long groupId, AssignRequest request) {
        User trainer = trainer(email, false);
        ownedGroup(trainer, groupId, false);
        // Lock all affected users in a deterministic order before group/member rows.
        // Copy insertion references the recipient user, so locking it after the group
        // could deadlock against account deletion and its FK cascades.
        Set<Long> candidateIds;
        if (request.userId() != null) {
            candidateIds = Set.of(acceptedMember(groupId, request.userId()).getUserId());
        } else {
            candidateIds = members.findAllByGroupIdOrderByInvitedAtAscUserIdAsc(groupId).stream()
                    .filter(GroupMember::isAccepted).map(GroupMember::getUserId).collect(Collectors.toSet());
        }
        lockUsers(Stream.concat(Stream.of(trainer.getId()), candidateIds.stream()).toList());
        trainer = trainer(email, false);
        TrainingGroup group = ownedGroup(trainer, groupId, true);
        List<GroupMember> lockedMembers = members.findAllForUpdate(groupId);
        // The initial recipient scan can have loaded the entities before a concurrent
        // acceptance. Refresh their state while holding the pessimistic row locks.
        lockedMembers.forEach(member -> entityManager.refresh(member, LockModeType.PESSIMISTIC_WRITE));
        List<GroupMember> recipients = lockedMembers.stream().filter(GroupMember::isAccepted)
                .filter(member -> request.userId() == null || member.getUserId().equals(request.userId()))
                .filter(member -> users.findById(member.getUserId()).map(User::isEnabled).orElse(false)).toList();
        if (request.userId() != null && recipients.isEmpty()) throw missing("Nie znaleziono aktywnego podopiecznego w tej grupie");
        if (recipients.stream().anyMatch(member -> !candidateIds.contains(member.getUserId()))) {
            throw conflict("Skład grupy zmienił się. Odśwież grupę i ponów przypisanie planu");
        }
        if (recipients.isEmpty()) throw conflict("Najpierw podopieczny musi zaakceptować zaproszenie");
        WorkoutPlan source = plans.findByIdAndOwnerId(request.planId(), trainer.getId())
                .orElseThrow(() -> missing("Nie znaleziono własnego planu"));
        User lockedTrainer = trainer;
        return recipients.stream().map(member -> assignCopy(group, lockedTrainer, member, source)).toList();
    }

    public List<AssignmentResponse> assignments(String email) {
        return assignments.findAllByUserIdOrderByAssignedAtDescIdDesc(currentUser(email, false).getId())
                .stream().map(this::assignmentResponse).toList();
    }

    public List<ExerciseResultResponse> results(String email, Long groupId, Long userId,
            Long exerciseId, Instant from, Instant to) {
        ownedGroup(trainer(email, false), groupId, false);
        acceptedMember(groupId, userId);
        return progress.listResultsForUser(userId, exerciseId, from, to);
    }

    public List<BodyMetricResponse> metrics(String email, Long groupId, Long userId, Instant from, Instant to) {
        ownedGroup(trainer(email, false), groupId, false);
        acceptedMember(groupId, userId);
        return progress.listMetricsForUser(userId, from, to);
    }

    private AssignmentResponse assignCopy(TrainingGroup group, User trainer, GroupMember member, WorkoutPlan source) {
        User trainee = users.findById(member.getUserId()).filter(User::isEnabled)
                .orElseThrow(() -> missing("Nie znaleziono podopiecznego"));
        PlanAssignment assignment = assignments.findByGroupIdAndUserIdAndSourcePlanId(
                group.getId(), trainee.getId(), source.getId()).orElseGet(PlanAssignment::new);
        // Retrying a submitted form must not create a second plan copy.
        if (assignment.getCopiedPlanId() != null && plans.existsById(assignment.getCopiedPlanId())) {
            return assignmentResponse(assignment);
        }
        WorkoutPlan copy = new WorkoutPlan();
        copy.setOwner(trainee);
        copy.setName(source.getName());
        copy.setDescription(source.getDescription());
        for (PlanDay sourceDay : source.getDays()) {
            PlanDay day = new PlanDay();
            day.setName(sourceDay.getName());
            day.setDayIndex(sourceDay.getDayIndex());
            copy.addDay(day);
            for (PlanExercise sourceExercise : sourceDay.getExercises()) {
                PlanExercise exercise = new PlanExercise();
                exercise.setExercise(sourceExercise.getExercise());
                exercise.setOrderIndex(sourceExercise.getOrderIndex());
                exercise.setSets(sourceExercise.getSets());
                exercise.setReps(sourceExercise.getReps());
                exercise.setWeight(sourceExercise.getWeight());
                day.addExercise(exercise);
            }
        }
        plans.saveAndFlush(copy);
        assignment.setGroupId(group.getId());
        assignment.setUserId(trainee.getId());
        assignment.setSourcePlanId(source.getId());
        assignment.setCopiedPlanId(copy.getId());
        assignment.setAssignedBy(trainer.getId());
        assignment.setPlanName(copy.getName());
        assignment.refreshAssignedAt();
        return assignmentResponse(assignments.saveAndFlush(assignment));
    }

    private GroupResponse groupResponse(TrainingGroup group) {
        var responses = members.findAllByGroupIdOrderByInvitedAtAscUserIdAsc(group.getId()).stream().map(member -> {
            User user = users.findById(member.getUserId()).orElseThrow();
            return new MemberResponse(user.getId(), user.getDisplayName(), user.getEmail(), member.getStatus(),
                    member.getInvitedAt(), member.getAcceptedAt());
        }).toList();
        return new GroupResponse(group.getId(), group.getName(), group.getCreatedAt(), responses);
    }

    private AssignmentResponse assignmentResponse(PlanAssignment assignment) {
        User trainee = users.findById(assignment.getUserId()).orElseThrow();
        User trainer = users.findById(assignment.getAssignedBy()).orElseThrow();
        TrainingGroup group = groups.findById(assignment.getGroupId()).orElseThrow();
        return new AssignmentResponse(assignment.getId(), assignment.getCopiedPlanId(), assignment.getPlanName(),
                trainer.getDisplayName(), group.getName(), trainee.getId(), trainee.getDisplayName(), assignment.getAssignedAt());
    }

    private GroupMember acceptedMember(Long groupId, Long userId) {
        GroupMember member = member(groupId, userId);
        if (!member.isAccepted() || !users.findById(userId).map(User::isEnabled).orElse(false)) {
            throw missing("Nie znaleziono aktywnego podopiecznego w tej grupie");
        }
        return member;
    }

    private GroupMember member(Long groupId, Long userId) {
        return members.findById(new GroupMemberId(groupId, userId))
                .orElseThrow(() -> missing("Nie znaleziono członkostwa w grupie"));
    }

    private GroupMember lockedMember(Long groupId, Long userId) {
        return members.findForUpdate(groupId, userId)
                .orElseThrow(() -> missing("Nie znaleziono członkostwa w grupie"));
    }

    private void lockUsers(List<Long> userIds) {
        for (Long userId : userIds.stream().distinct().sorted().toList()) {
            User user = users.findForUpdateById(userId)
                    .orElseThrow(() -> conflict("Konto zostało usunięte. Odśwież grupę i spróbuj ponownie"));
            entityManager.refresh(user, LockModeType.PESSIMISTIC_WRITE);
        }
    }

    private TrainingGroup ownedGroup(User trainer, Long id, boolean lock) {
        var result = lock ? groups.findOwnedForUpdate(id, trainer.getId()) : groups.findByIdAndTrainerId(id, trainer.getId());
        return result.orElseThrow(() -> missing("Nie znaleziono grupy"));
    }

    private User trainer(String email, boolean lock) {
        User user = currentUser(email, lock);
        if (user.getRole() != UserRole.TRAINER) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Brak uprawnień trenera");
        return user;
    }

    private boolean activeTrainer(Long userId) {
        return users.findById(userId).filter(User::isEnabled).filter(user -> user.getRole() == UserRole.TRAINER).isPresent();
    }

    private User currentUser(String email, boolean lock) {
        var result = lock ? users.findForUpdateByEmail(email) : users.findByEmailIgnoreCase(email);
        return result.filter(User::isEnabled)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Zaloguj się ponownie"));
    }

    private ResponseStatusException missing(String message) { return new ResponseStatusException(HttpStatus.NOT_FOUND, message); }
    private ResponseStatusException conflict(String message) { return new ResponseStatusException(HttpStatus.CONFLICT, message); }
}
