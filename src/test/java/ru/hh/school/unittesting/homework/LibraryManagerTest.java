package ru.hh.school.unittesting.homework;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.math.RoundingMode;

@ExtendWith(MockitoExtension.class)
public class LibraryManagerTest {

    @Mock
    NotificationService notificationService;

    @Mock
    UserService userService;

    @InjectMocks
    LibraryManager libraryManager;

    @BeforeEach
    void addExistingBook() {
        libraryManager.addBook("existingBook", 1);
    }

    @Test
    void testAddNewBook() {
        libraryManager.addBook("newBook", 1000);
        int totalQuantity = libraryManager.getAvailableCopies("newBook");
        Assertions.assertEquals(
            1000,
            totalQuantity
        );
    }

    @Test
    void testAddExistingBook() {
        libraryManager.addBook("existingBook", 10);
        int totalQuantity = libraryManager.getAvailableCopies("existingBook");
        Assertions.assertEquals(
            11,
            totalQuantity
        );
    }

    @Test
    void testLibraryManagerReturnsZeroWhenBookDoesNotExist() {
        int totalQuantity = libraryManager.getAvailableCopies("bookThatDoesNotExist");
        Assertions.assertEquals(
            0,
            totalQuantity
        );
    }

    @Test
    void testCanNotBorrowBookWhenUserIsNotActive() {
        libraryManager.addBook("book", 1);
        Assertions.assertFalse(
            libraryManager.borrowBook("book", "inactiveUser")
        );
    }

    @Test
    void testCanNotBorrowBookWhenQuantityIsNotEnough() {
        Mockito.when(userService.isUserActive("activeUser")).thenReturn(true);
        Assertions.assertFalse(
            libraryManager.borrowBook("bookThatIsAbsent", "activeUser")
        );
    }

    @ParameterizedTest
    @CsvSource({
        "book1, 1, user12",
        "book2, 2, user123",
        "book3, 123456789, abobusUser"
    })
    void testCanBorrowBookAndResultIsCorrect(String bookId, int beforeQuantity, String userId) {
        Mockito.when(userService.isUserActive(ArgumentMatchers.anyString())).thenReturn(true);
        libraryManager.addBook(bookId, beforeQuantity);

        Assertions.assertTrue(
            libraryManager.borrowBook(bookId, userId)
        );
        int afterQuantity = libraryManager.getAvailableCopies(bookId);
        Assertions.assertEquals(beforeQuantity - 1, afterQuantity);
    }

    @Test
    void testTwoUsersCanBorrowSameBookAndResultIsCorrect() {
        Mockito.when(userService.isUserActive(ArgumentMatchers.anyString())).thenReturn(true);
        libraryManager.addBook("book", 3);
        Assertions.assertAll(
            () -> Assertions.assertTrue(libraryManager.borrowBook("book", "user1")),
            () -> Assertions.assertTrue(libraryManager.borrowBook("book", "user2")),
            () -> Assertions.assertEquals(
                1,
                libraryManager.getAvailableCopies("book")
            )
        );
    }

    @Test
    void testCanOneUserBorrowSameBookTwiceAndResultIsCorrect() {
        Mockito.when(userService.isUserActive(ArgumentMatchers.anyString())).thenReturn(true);
        libraryManager.addBook("book1", 3);
        Assertions.assertAll(
            () -> Assertions.assertTrue(libraryManager.borrowBook("book1", "user")),
            () -> Assertions.assertTrue(libraryManager.borrowBook("book1", "user")),
            () -> Assertions.assertEquals(
                1,
                libraryManager.getAvailableCopies("book1")
            )
        );
    }

    @Test
    void testCanNotReturnBookWhenBookWasNotBorrowed() {
        libraryManager.borrowBook("existingBook", "user");
        Assertions.assertFalse(
            libraryManager.borrowBook("bookThatWasNotBorrowed", "user")
        );
    }

    @Test
    void testCanNotReturnBookThatWasBorrowedByAnotherUser() {
        Mockito.when(userService.isUserActive(ArgumentMatchers.anyString())).thenReturn(true);
        libraryManager.borrowBook("existingBook", "user1");
        Assertions.assertFalse(
            libraryManager.returnBook("existingBook", "user2")
        );
    }

    @ParameterizedTest
    @CsvSource({
        "book1, 10, userAbuser",
        "boooook, 1, user1",
        ", 12, user",
    })
    void testCanReturnBookAndResultIsCorrect(String bookId, int beforeQuantity, String userId) {
        Mockito.when(userService.isUserActive(ArgumentMatchers.anyString())).thenReturn(true);
        libraryManager.addBook(bookId, beforeQuantity);
        libraryManager.borrowBook(bookId, userId);

        Assertions.assertTrue(
            libraryManager.returnBook(bookId, userId)
        );
        int afterQuantity = libraryManager.getAvailableCopies(bookId);
        Assertions.assertEquals(beforeQuantity, afterQuantity);
    }

    @Test
    void testCanUserReturnSameBookTwice() {
        Mockito.when(userService.isUserActive(ArgumentMatchers.anyString())).thenReturn(true);
        libraryManager.addBook("book1", 3);
        libraryManager.borrowBook("book1", "user1");
        libraryManager.borrowBook("book1", "user1");
        Assertions.assertTrue(libraryManager.returnBook("book1", "user1"));
        Assertions.assertTrue(libraryManager.returnBook("book1", "user1"));
        Assertions.assertEquals(3, libraryManager.getAvailableCopies("book1"));
    }

    @Test
    void testCanTwoUsersReturnSameBook() {
        Mockito.when(userService.isUserActive(ArgumentMatchers.anyString())).thenReturn(true);
        libraryManager.addBook("book1", 3);
        libraryManager.borrowBook("book1", "user1");
        libraryManager.borrowBook("book1", "user2");
        Assertions.assertTrue(libraryManager.returnBook("book1", "user1"));
        Assertions.assertTrue(libraryManager.returnBook("book1", "user2"));
        Assertions.assertEquals(3, libraryManager.getAvailableCopies("book1"));
    }

    @Test
    void testThrowsExceptionWhenOverdueDaysIsNegative() {
        var exception = Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> libraryManager.calculateDynamicLateFee(
                    -1,
                    true,
                    true
            )
        );
        Assertions.assertEquals("Overdue days cannot be negative.", exception.getMessage());
    }

    @ParameterizedTest
    @ValueSource(
        ints = {0, 1, 2, 3, 10, 33, 50}
    )
    void testCalculateDynamicLateFeeWithoutBestsellerAndPremiumMember(int overdueDays) {
        double fee = libraryManager.calculateDynamicLateFee(
                overdueDays, false, false);
        Assertions.assertEquals(
                BigDecimal.valueOf(overdueDays * 0.5)
                        .setScale(2, RoundingMode.HALF_UP).doubleValue(),
                fee
        );
    }

    @ParameterizedTest
    @ValueSource(
            ints = {0, 1, 2, 3, 10, 33, 50}
    )
    void testCalculateDynamicLateFeeWithBestsellerButWithoutPremiumMember(int overdueDays) {
        double fee = libraryManager.calculateDynamicLateFee(
                overdueDays, true, false);
        Assertions.assertEquals(
                BigDecimal.valueOf(overdueDays * 0.5 * 1.5)
                        .setScale(2, RoundingMode.HALF_UP).doubleValue(),
                fee
        );
    }

    @ParameterizedTest
    @ValueSource(
            ints = {0, 1, 2, 3, 10, 33, 50}
    )
    void testCalculateDynamicLateFeeWithPremiumMemberButWithoutBestseller(int overdueDays) {
        double fee = libraryManager.calculateDynamicLateFee(
                overdueDays, false, true);
        Assertions.assertEquals(
                BigDecimal.valueOf(overdueDays * 0.5 * 0.8)
                        .setScale(2, RoundingMode.HALF_UP).doubleValue(),
                fee
        );
    }

    @ParameterizedTest
    @ValueSource(
            ints = {0, 1, 2, 3, 10, 33, 50}
    )
    void testCalculateDynamicLateFeeWithBestsellerAndPremiumMember(int overdueDays) {
        double fee = libraryManager.calculateDynamicLateFee(
                overdueDays, true, true);
        Assertions.assertEquals(
                BigDecimal.valueOf(overdueDays * 0.5 * 1.5 * 0.8)
                        .setScale(2, RoundingMode.HALF_UP).doubleValue(),
                fee
        );
    }
}
