package ru.hh.school.unittesting.homework;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LibraryManagerTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private UserService userService;

    @InjectMocks
    private LibraryManager libraryManager;

    @BeforeEach
    void setUp(){
        libraryManager.addBook("1", 10);
    }

    @Test
    void shouldIncreaseQuantityForExistingBook() {
        libraryManager.addBook("1", 10);
        assertEquals(20, libraryManager.getAvailableCopies("1"));

    }

    @Test
    void shouldIncreaseQuantityForNonExistingBook() {
        libraryManager.addBook("2", 10);
        assertEquals(10, libraryManager.getAvailableCopies("2"));
    }



    @Test
    void shouldNotBorrowExistingBookByNotActiveUser() {
        when(userService.isUserActive("1")).thenReturn(false);
        assertEquals(
                false,
                libraryManager.borrowBook("1", "1")
        );
    }

    @Test
    void shouldNotBorrowNonExistingBookByNotActiveUser() {
        when(userService.isUserActive("1")).thenReturn(false);
        assertEquals(
                false,
                libraryManager.borrowBook("2", "1")
        );
    }

    @Test
    void shouldBorrowExistingBookByActiveUser() {
        when(userService.isUserActive("1")).thenReturn(true);
        assertTrue(libraryManager.borrowBook("1", "1"));
        assertEquals(9, libraryManager.getAvailableCopies("1"));
    }

    @Test
    void shouldNotBorrowNonExistingBookByActiveUser() {
        when(userService.isUserActive("1")).thenReturn(true);
        assertEquals(false, libraryManager.borrowBook("2", "1"));
    }

    @Test
    void shouldNotReturnNonExistingBook() {
        assertFalse(libraryManager.returnBook("2", "1"));
    }

    @Test
    void shouldNotReturnNotBorrowedBook() {
        assertFalse(libraryManager.returnBook("1", "1"));
    }

    @Test
    void shouldReturnBorrowedBook() {
        when(userService.isUserActive("1")).thenReturn(true);
        libraryManager.borrowBook("1", "1");

        assertTrue(libraryManager.returnBook("1", "1"));
        assertEquals(10, libraryManager.getAvailableCopies("1"));
    }

    @Test
    void testGetAvailableCopiesForExistingBook() {
        assertEquals(10, libraryManager.getAvailableCopies("1"));
    }

    @Test
    void testGetAvailableCopiesForNonExistingBook() {
        assertEquals(0, libraryManager.getAvailableCopies("2"));
    }

    @Test
    void shouldThrowExceptionWhenOverdueDaysIsNegative() {
        assertThrows(
                IllegalArgumentException.class,
                () -> libraryManager.calculateDynamicLateFee(-1, true, true)
        );
    }

    @ParameterizedTest
    @CsvSource({
            "0, false, false",
            "1, true, false",
            "2, false, true",
            "3, true, true",
    })
    void testCalculateDynamicLateFeeWithCorrectData(int overdueDays, boolean isBestseller, boolean isPremiumMember) {
        double result = BigDecimal.valueOf(
                    overdueDays * 0.5 * (isBestseller ? 1.5 : 1) * (isPremiumMember ? 0.8 : 1)
                ).setScale(2, RoundingMode.HALF_UP)
                .doubleValue();

        assertEquals(
                result,
                libraryManager.calculateDynamicLateFee(overdueDays, isBestseller, isPremiumMember)
        );
    }

}
