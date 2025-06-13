package com.example.onlinestore.aspect;

import com.example.onlinestore.dto.UserPageRequest;
import jakarta.validation.Validator;
import jakarta.validation.ConstraintViolation;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
// MessageSource and ResponseEntity no longer needed for failure case assertions here
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException; // Added import
import jakarta.validation.Validator;

import java.util.HashSet;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("参数验证切面测试")
public class ValidationAspectTest {

    @Mock
    private Validator validator;

    // MessageSource mock is no longer needed
    // @Mock
    // private MessageSource messageSource;

    @Mock
    private ProceedingJoinPoint joinPoint;
    @Mock
    private Signature signature; // Mock signature for logging

    @InjectMocks
    private ValidationAspect validationAspect;

    private UserPageRequest request;

    @BeforeEach
    void setUp() {
        request = new UserPageRequest();
        request.setPageNum(0); // Invalid page number for testing
        request.setPageSize(10);
        when(joinPoint.getSignature()).thenReturn(signature); // Mock signature for logging
        when(signature.toShortString()).thenReturn("TestClass.testMethod()");
    }

    @Nested
    @DisplayName("参数验证测试")
    class ValidationTests {
        @Test
        @DisplayName("验证通过")
        void whenValidationPasses_thenProceedWithMethod() throws Throwable {
            Object expectedResult = new Object(); // Placeholder for actual expected result
            Object[] args = new Object[]{request};
            when(joinPoint.getArgs()).thenReturn(args);
            when(validator.validate(request)).thenReturn(new HashSet<>()); // No violations
            when(joinPoint.proceed()).thenReturn(expectedResult);

            Object result = validationAspect.validateParameters(joinPoint);

            verify(joinPoint).proceed();
            assertEquals(expectedResult, result);
        }

        @Test
        @DisplayName("验证失败时抛出ConstraintViolationException")
        void whenValidationFails_thenThrowConstraintViolationException() throws Throwable {
            Object[] args = new Object[]{request};
            when(joinPoint.getArgs()).thenReturn(args);

            Set<ConstraintViolation<Object>> violations = new HashSet<>();
            @SuppressWarnings("unchecked") // Suppress warning for mock type
            ConstraintViolation<Object> violation = mock(ConstraintViolation.class);
            violations.add(violation);
            
            when(validator.validate(request)).thenReturn(violations);
            // No need to mock messageSource anymore

            ConstraintViolationException exception = assertThrows(
                ConstraintViolationException.class,
                () -> validationAspect.validateParameters(joinPoint)
            );

            verify(joinPoint, never()).proceed();
            assertEquals(violations, exception.getConstraintViolations());
        }

        @Test
        @DisplayName("参数为空时跳过验证")
        void whenArgumentIsNull_thenSkipValidationAndProceed() throws Throwable {
            Object expectedResult = new Object();
            Object[] args = new Object[]{null}; // Argument is null
            when(joinPoint.getArgs()).thenReturn(args);
            when(joinPoint.proceed()).thenReturn(expectedResult);

            Object result = validationAspect.validateParameters(joinPoint);

            verify(validator, never()).validate(any());
            verify(joinPoint).proceed();
            assertEquals(expectedResult, result);
        }

        @Test
        @DisplayName("多个验证错误时抛出包含所有错误的Exception")
        void whenMultipleValidationErrors_thenExceptionContainsAllViolations() throws Throwable {
            Object[] args = new Object[]{request};
            when(joinPoint.getArgs()).thenReturn(args);

            Set<ConstraintViolation<Object>> violations = new HashSet<>();
            @SuppressWarnings("unchecked")
            ConstraintViolation<Object> violation1 = mock(ConstraintViolation.class);
            @SuppressWarnings("unchecked")
            ConstraintViolation<Object> violation2 = mock(ConstraintViolation.class);
            violations.add(violation1);
            violations.add(violation2);
            
            when(validator.validate(request)).thenReturn(violations);

            ConstraintViolationException exception = assertThrows(
                ConstraintViolationException.class,
                () -> validationAspect.validateParameters(joinPoint)
            );

            verify(joinPoint, never()).proceed();
            assertEquals(violations, exception.getConstraintViolations());
            assertTrue(exception.getConstraintViolations().contains(violation1));
            assertTrue(exception.getConstraintViolations().contains(violation2));
        }

        @Test
        @DisplayName("原方法抛出异常时重新抛出")
        void whenProceedThrowsException_thenRethrowException() throws Throwable {
            // 准备测试数据
            Object[] args = new Object[]{request};
            when(joinPoint.getArgs()).thenReturn(args);
            when(validator.validate(any())).thenReturn(new HashSet<>());
            when(joinPoint.proceed()).thenThrow(new RuntimeException("Test exception"));

            // 执行测试并验证异常
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> validationAspect.validateParameters(joinPoint));
            assertEquals("Test exception", exception.getMessage());
        }
    }
} 