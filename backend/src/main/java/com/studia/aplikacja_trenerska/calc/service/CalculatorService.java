package com.studia.aplikacja_trenerska.calc.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.IntStream;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.studia.aplikacja_trenerska.calc.dto.CalcDtos.ActivityLevel;
import com.studia.aplikacja_trenerska.calc.dto.CalcDtos.CalorieResponse;
import com.studia.aplikacja_trenerska.calc.dto.CalcDtos.Goal;
import com.studia.aplikacja_trenerska.calc.dto.CalcDtos.Macros;
import com.studia.aplikacja_trenerska.calc.dto.CalcDtos.OneRepMaxResponse;
import com.studia.aplikacja_trenerska.calc.dto.CalcDtos.Percentage;
import com.studia.aplikacja_trenerska.calc.dto.CalcDtos.Sex;

@Service
public class CalculatorService {
    public static final int MAX_REPS = 20;
    private static final BigDecimal MAX_LIFT = new BigDecimal("2000");
    private static final BigDecimal MIN_BODY_WEIGHT = new BigDecimal("30");
    private static final BigDecimal MAX_BODY_WEIGHT = new BigDecimal("300");
    private static final int[] PERCENTS = {100, 95, 90, 85, 80, 75, 70, 65, 60, 55, 50};

    public OneRepMaxResponse oneRepMax(BigDecimal weight, int reps) {
        if (weight.signum() <= 0 || weight.compareTo(MAX_LIFT) > 0 || weight.stripTrailingZeros().scale() > 2) {
            throw badRequest("Ciężar musi być większy od 0 i nie większy niż 2000 kg, maksymalnie z dwoma miejscami po przecinku");
        }
        if (reps < 1 || reps > MAX_REPS) {
            throw badRequest("Liczba powtórzeń musi mieścić się w zakresie 1–" + MAX_REPS);
        }
        double lifted = weight.doubleValue();
        double epley = reps == 1 ? lifted : lifted * (1 + reps / 30.0);
        double brzycki = lifted * 36 / (37 - reps);
        double lombardi = lifted * Math.pow(reps, 0.10);
        List<Percentage> percentages = IntStream.of(PERCENTS).mapToObj(percent -> new Percentage(percent,
                toHalfKilogram(epley * percent / 100), percent == 100 ? 1 : (int) Math.max(1, Math.round(30 * (100.0 / percent - 1)))))
                .toList();
        return new OneRepMaxResponse(weight, reps, toTenth(epley), toTenth(brzycki), toTenth(lombardi), percentages);
    }

    public CalorieResponse calories(Sex sex, int age, int heightCm, BigDecimal weightKg, ActivityLevel activity, Goal goal) {
        if (age < 15 || age > 100) throw badRequest("Wiek musi mieścić się w zakresie 15–100 lat");
        if (heightCm < 100 || heightCm > 250) throw badRequest("Wzrost musi mieścić się w zakresie 100–250 cm");
        if (weightKg.compareTo(MIN_BODY_WEIGHT) < 0 || weightKg.compareTo(MAX_BODY_WEIGHT) > 0
                || weightKg.stripTrailingZeros().scale() > 2) {
            throw badRequest("Masa ciała musi mieścić się w zakresie 30–300 kg, maksymalnie z dwoma miejscami po przecinku");
        }
        double weight = weightKg.doubleValue();
        double bmr = 10 * weight + 6.25 * heightCm - 5 * age + (sex == Sex.MALE ? 5 : -161);
        double tdee = bmr * activity.factor();
        double target = switch (goal) {
            case LOSE -> tdee * 0.85;
            case MAINTAIN -> tdee;
            case GAIN -> tdee * 1.10;
        };
        int targetCalories = (int) Math.round(target);
        double proteinPerKg = switch (goal) {
            case LOSE -> 2.0;
            case MAINTAIN -> 1.6;
            case GAIN -> 1.8;
        };
        int proteinG = (int) Math.round(weight * proteinPerKg);
        int fatG = (int) Math.round(targetCalories * 0.25 / 9);
        int carbsG = Math.max(0, (int) Math.round((targetCalories - proteinG * 4 - fatG * 9) / 4.0));
        Macros macros = new Macros(proteinG, fatG, carbsG, proteinG * 4, fatG * 9, carbsG * 4);
        int roundedTdee = (int) Math.round(tdee);
        return new CalorieResponse((int) Math.round(bmr), roundedTdee, targetCalories, targetCalories - roundedTdee,
                activity.factor(), macros);
    }

    private static BigDecimal toTenth(double value) {
        return BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP);
    }

    private static BigDecimal toHalfKilogram(double value) {
        return BigDecimal.valueOf(Math.round(value * 2) / 2.0).setScale(1, RoundingMode.HALF_UP);
    }

    private static ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
