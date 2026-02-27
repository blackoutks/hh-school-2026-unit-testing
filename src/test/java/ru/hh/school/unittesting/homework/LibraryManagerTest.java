package ru.hh.school.unittesting.homework;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LibraryManagerTest {

  @InjectMocks
  private LibraryManager libraryManager;

  @Mock
  private NotificationService notificationService;

  @Mock
  private UserService userService;

  @BeforeEach
  void setUp() {
    libraryManager.addBook("testBook1", 5);
  }

  // конечно, вы говорили, что сеттеры можно не тестировать, но пусть будут (ради тренировки не помешает)
  @Test
  public void testAddBookShouldCreateNewUniqueBook() {
    libraryManager.addBook("testBook2", 1);
    assertEquals(1, libraryManager.getAvailableCopies("testBook2"));
  }

  @Test
  public void testAddBookShouldIncreaseAmountOfExitingBook() {
    libraryManager.addBook("testBook1", 1);
    assertEquals(6, libraryManager.getAvailableCopies("testBook1"));
  }

  @Disabled
  @Test
  public void testAddBookShouldReturnTrueWithEmptyName() {
    /*
      ладно, тест тупо для вопроса ), понятно что тут логики нет для этого и он неверный,
      а как такое нормально обрабатывать в нормальном продакшене, валидатор писать?
      просто там и с null всё будет отлично работать и не только в этой функции, а это не порядок
     */
    libraryManager.addBook("", 1);
    assertEquals(1, libraryManager.getAvailableCopies(""));
  }

  // в общем, ладно, наконец-то к нормальным тестам перейдём
  @Test
  public void testBorrowBookShouldReturnTrueDecreaseBookAndNotifyWithSuccess() {
    when(userService.isUserActive("testUser1")).thenReturn(true);
    boolean testResult = libraryManager.borrowBook("testBook1", "testUser1");
    assertTrue(testResult);
    assertEquals(4, libraryManager.getAvailableCopies("testBook1"));
    verify(notificationService).notifyUser("testUser1", "You have borrowed the book: testBook1");
  }

  @Test
  public void testBorrowBookShouldReturnFalseAndNotifyIfUnactiveUser() {
    when(userService.isUserActive("testUser2")).thenReturn(false);
    boolean testResult = libraryManager.borrowBook("testBook1", "testUser2");
    assertFalse(testResult);
    assertEquals(5, libraryManager.getAvailableCopies("testBook1"));
    verify(notificationService).notifyUser("testUser2", "Your account is not active.");
  }

  @Test
  public void testBorrowBookShouldReturnFalseOnCheckZeroBooksAmountOfBooks() {
    libraryManager.addBook("testBook2", 0);
    when(userService.isUserActive("testUser1")).thenReturn(true);
    boolean testResult = libraryManager.borrowBook("testBook2", "testUser1");
    assertFalse(testResult);
    assertEquals(0, libraryManager.getAvailableCopies("testBook2"));
  }

  @Test
  public void testBorrowBookShouldReturnFalseOnCheckBelowZeroBooksAmountOfBooks() {
    libraryManager.addBook("testBook2", -11);
    when(userService.isUserActive("testUser1")).thenReturn(true);
    boolean testResult = libraryManager.borrowBook("testBook2", "testUser1");
    assertFalse(testResult);
    assertEquals(-11, libraryManager.getAvailableCopies("testBook2"));
  }

  @Test
  public void testReturnBookShouldReturnTrueIncreaseBookAndNotifyWithSuccess() {
    when(userService.isUserActive("testUser1")).thenReturn(true);
    libraryManager.borrowBook("testBook1", "testUser1");
    boolean testResult = libraryManager.returnBook("testBook1", "testUser1");
    assertEquals(5, libraryManager.getAvailableCopies("testBook1"));
    assertTrue(testResult);
    verify(notificationService).notifyUser("testUser1", "You have returned the book: testBook1");
  }

  @Test
  public void testReturnBookShouldReturnFalseIfNotEqualsValue() {
    when(userService.isUserActive("testUser1")).thenReturn(true);
    libraryManager.borrowBook("testBook1", "testUser1");
    boolean testResult = libraryManager.returnBook("testBook1", "testUser2");
    assertEquals(4, libraryManager.getAvailableCopies("testBook1"));
    assertFalse(testResult);
  }

  @Test
  public void testReturnBookShouldReturnFalseIfNotContainsKey() {
    boolean testResult = libraryManager.returnBook("testBook1", "testUser1");
    assertEquals(5, libraryManager.getAvailableCopies("testBook1"));
    assertFalse(testResult);
  }

  @Disabled
  @Test
  public void testShouldReturnTrueDespiteRebaseMapaValue() {
    // он здесь, чтобы показать что такая проблема есть в коде (с заменой элемента в мапе)
    libraryManager.addBook("testBook2", 4);
    when(userService.isUserActive("testUser1")).thenReturn(true);
    when(userService.isUserActive("testUser2")).thenReturn(true);
    boolean testResult1 = libraryManager.borrowBook("testBook2", "testUser1");
    assertTrue(testResult1);
    boolean testResult2 = libraryManager.borrowBook("testBook2", "testUser2");
    assertTrue(testResult2);
    boolean testResult3 = libraryManager.returnBook("testBook2", "testUser1");
    assertTrue(testResult3);
    assertEquals(3, libraryManager.getAvailableCopies("testBook2"));
  }

  @ParameterizedTest
  @CsvSource({
      "10, false, false, 5.",
      "10, false, true, 4.",
      "10, true, false, 7.5",
      "10, true, true, 6.",
      "0, true, true, 0.",
      "1, true, false, 0.75"
      // пытался добиться 3 знаков после запятой, но там константы удачные
  })
  public void testCalculateDynamicLateFeeShouldReturnTrueWithSuccess(int testOverDueDays,
                                                 boolean testIsBestseller,
                                                 boolean testIsPremium,
                                                 double testExpected) {
    assertEquals(testExpected, libraryManager.calculateDynamicLateFee(testOverDueDays, testIsBestseller, testIsPremium), 0.001);
  }

  @ParameterizedTest
  @CsvSource({
      "-1111, false, false",
      "-1111, false, true",
      "-1111, true, false",
      "-1111, true, true",
  })
  public void testCalculateDynamicLateFeeShouldThrowsExceptionIfOverDaysBelowZero(int testOverDueDays,
                                                           boolean testIsBestseller,
                                                           boolean testIsPremium) {
    assertThrows(IllegalArgumentException.class, () -> libraryManager.calculateDynamicLateFee(testOverDueDays, testIsBestseller, testIsPremium));
  }
}
