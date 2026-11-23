package com.studia.aplikacja_trenerska.calc.controller;

import java.math.BigDecimal;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.studia.aplikacja_trenerska.calc.dto.CalcDtos.ActivityLevel;
import com.studia.aplikacja_trenerska.calc.dto.CalcDtos.CalorieResponse;
import com.studia.aplikacja_trenerska.calc.dto.CalcDtos.Goal;
import com.studia.aplikacja_trenerska.calc.dto.CalcDtos.OneRepMaxResponse;
import com.studia.aplikacja_trenerska.calc.dto.CalcDtos.Sex;
import com.studia.aplikacja_trenerska.calc.service.CalculatorService;

@RestController
@RequestMapping("/api/calc")
@PreAuthorize("hasAnyRole('USER', 'TRAINER', 'ADMIN')")
public class CalculatorController {
    private final CalculatorService calculator;

    public CalculatorController(CalculatorService calculator) {
        this.calculator = calculator;
    }

    @GetMapping("/1rm")
    public OneRepMaxResponse oneRepMax(@RequestParam BigDecimal weight, @RequestParam int reps) {
        return calculator.oneRepMax(weight, reps);
    }

    @GetMapping("/calories")
    public CalorieResponse calories(@RequestParam Sex sex, @RequestParam int age, @RequestParam int heightCm,
            @RequestParam BigDecimal weightKg, @RequestParam ActivityLevel activity, @RequestParam Goal goal) {
        return calculator.calories(sex, age, heightCm, weightKg, activity, goal);
    }
}
