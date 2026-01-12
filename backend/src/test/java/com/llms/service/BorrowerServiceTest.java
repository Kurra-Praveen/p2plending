package com.llms.service;

import com.llms.dto.request.CreateBorrowerRequest;
import com.llms.dto.request.UpdateBorrowerRequest;
import com.llms.dto.response.BorrowerResponse;
import com.llms.entity.Borrower;
import com.llms.entity.User;
import com.llms.enums.BorrowerStatus;
import com.llms.enums.UserRole;
import com.llms.exception.DuplicateResourceException;
import com.llms.exception.ResourceNotFoundException;
import com.llms.repository.BorrowerRepository;
import com.llms.security.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BorrowerService Unit Tests")
class BorrowerServiceTest {

    @Mock
    private BorrowerRepository borrowerRepository;

    @Mock
    private SecurityUtils securityUtils;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private BorrowerService borrowerService;

    private User currentUser;
    private Borrower testBorrower;

    @BeforeEach
    void setUp() {
        currentUser = User.builder()
                .id(UUID.randomUUID())
                .name("Test Lender")
                .email("lender@test.com")
                .role(UserRole.LENDER)
                .build();

        testBorrower = Borrower.builder()
                .id(UUID.randomUUID())
                .fullName("Test Borrower")
                .phone("9876543210")
                .email("borrower@test.com")
                .address("Test Address")
                .riskScore(650)
                .status(BorrowerStatus.ACTIVE)
                .createdBy(currentUser)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("createBorrower")
    class CreateBorrowerTests {

        @Test
        @DisplayName("Should create borrower successfully")
        void shouldCreateBorrowerSuccessfully() {
            CreateBorrowerRequest request = CreateBorrowerRequest.builder()
                    .fullName("New Borrower")
                    .phone("9999999999")
                    .email("new@test.com")
                    .address("New Address")
                    .riskScore(700)
                    .build();

            when(borrowerRepository.existsByPhone(request.getPhone())).thenReturn(false);
            when(securityUtils.getCurrentUser()).thenReturn(currentUser);
            when(borrowerRepository.save(any(Borrower.class))).thenAnswer(invocation -> {
                Borrower b = invocation.getArgument(0);
                b.setId(UUID.randomUUID());
                return b;
            });

            BorrowerResponse response = borrowerService.createBorrower(request);

            assertThat(response).isNotNull();
            assertThat(response.getFullName()).isEqualTo("New Borrower");
            assertThat(response.getPhone()).isEqualTo("9999999999");
            assertThat(response.getRiskScore()).isEqualTo(700);
            assertThat(response.getStatus()).isEqualTo(BorrowerStatus.ACTIVE);

            verify(borrowerRepository).save(any(Borrower.class));
            verify(auditService).logCreate(eq("Borrower"), any(UUID.class), any());
        }

        @Test
        @DisplayName("Should throw exception for duplicate phone")
        void shouldThrowExceptionForDuplicatePhone() {
            CreateBorrowerRequest request = CreateBorrowerRequest.builder()
                    .fullName("Duplicate")
                    .phone("9876543210")
                    .build();

            when(borrowerRepository.existsByPhone(request.getPhone())).thenReturn(true);

            assertThatThrownBy(() -> borrowerService.createBorrower(request))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("9876543210");

            verify(borrowerRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should set default risk score when not provided")
        void shouldSetDefaultRiskScore() {
            CreateBorrowerRequest request = CreateBorrowerRequest.builder()
                    .fullName("No Score")
                    .phone("8888888888")
                    .build();

            when(borrowerRepository.existsByPhone(any())).thenReturn(false);
            when(securityUtils.getCurrentUser()).thenReturn(currentUser);
            when(borrowerRepository.save(any(Borrower.class))).thenAnswer(invocation -> {
                Borrower b = invocation.getArgument(0);
                b.setId(UUID.randomUUID());
                return b;
            });

            BorrowerResponse response = borrowerService.createBorrower(request);

            assertThat(response.getRiskScore()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("getBorrower")
    class GetBorrowerTests {

        @Test
        @DisplayName("Should get borrower by ID")
        void shouldGetBorrowerById() {
            when(securityUtils.getCurrentUserId()).thenReturn(currentUser.getId());
            when(borrowerRepository.findByIdAndCreatedByAndNotDeleted(testBorrower.getId(), currentUser.getId()))
                    .thenReturn(Optional.of(testBorrower));

            BorrowerResponse response = borrowerService.getBorrower(testBorrower.getId());

            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(testBorrower.getId());
            assertThat(response.getFullName()).isEqualTo("Test Borrower");
        }

        @Test
        @DisplayName("Should throw exception for non-existent borrower")
        void shouldThrowExceptionForNonExistent() {
            UUID randomId = UUID.randomUUID();
            when(securityUtils.getCurrentUserId()).thenReturn(currentUser.getId());
            when(borrowerRepository.findByIdAndCreatedByAndNotDeleted(randomId, currentUser.getId()))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> borrowerService.getBorrower(randomId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("getAllBorrowers")
    class GetAllBorrowersTests {

        @Test
        @DisplayName("Should get all borrowers with pagination")
        void shouldGetAllBorrowersWithPagination() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Borrower> borrowerPage = new PageImpl<>(List.of(testBorrower), pageable, 1);

            when(securityUtils.getCurrentUserId()).thenReturn(currentUser.getId());
            when(borrowerRepository.findAllByCreatedByAndNotDeleted(currentUser.getId(), pageable))
                    .thenReturn(borrowerPage);

            Page<BorrowerResponse> response = borrowerService.getAllBorrowers(pageable);

            assertThat(response.getContent()).hasSize(1);
            assertThat(response.getTotalElements()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("updateBorrower")
    class UpdateBorrowerTests {

        @Test
        @DisplayName("Should update borrower successfully")
        void shouldUpdateBorrowerSuccessfully() {
            UpdateBorrowerRequest request = UpdateBorrowerRequest.builder()
                    .fullName("Updated Name")
                    .email("updated@test.com")
                    .riskScore(800)
                    .build();

            when(securityUtils.getCurrentUserId()).thenReturn(currentUser.getId());
            when(borrowerRepository.findByIdAndCreatedByAndNotDeleted(testBorrower.getId(), currentUser.getId()))
                    .thenReturn(Optional.of(testBorrower));
            when(borrowerRepository.save(any(Borrower.class))).thenReturn(testBorrower);

            BorrowerResponse response = borrowerService.updateBorrower(testBorrower.getId(), request);

            assertThat(response).isNotNull();
            verify(borrowerRepository).save(any(Borrower.class));
            verify(auditService).logUpdate(eq("Borrower"), eq(testBorrower.getId()), any(), any());
        }

        @Test
        @DisplayName("Should update only provided fields")
        void shouldUpdateOnlyProvidedFields() {
            UpdateBorrowerRequest request = UpdateBorrowerRequest.builder()
                    .email("onlyemail@test.com")
                    .build();

            when(securityUtils.getCurrentUserId()).thenReturn(currentUser.getId());
            when(borrowerRepository.findByIdAndCreatedByAndNotDeleted(testBorrower.getId(), currentUser.getId()))
                    .thenReturn(Optional.of(testBorrower));
            when(borrowerRepository.save(any(Borrower.class))).thenReturn(testBorrower);

            borrowerService.updateBorrower(testBorrower.getId(), request);

            // Name should remain unchanged
            assertThat(testBorrower.getFullName()).isEqualTo("Test Borrower");
            assertThat(testBorrower.getEmail()).isEqualTo("onlyemail@test.com");
        }
    }

    @Nested
    @DisplayName("blockBorrower")
    class BlockBorrowerTests {

        @Test
        @DisplayName("Should block borrower successfully")
        void shouldBlockBorrower() {
            when(securityUtils.getCurrentUserId()).thenReturn(currentUser.getId());
            when(borrowerRepository.findByIdAndCreatedByAndNotDeleted(testBorrower.getId(), currentUser.getId()))
                    .thenReturn(Optional.of(testBorrower));
            when(borrowerRepository.save(any(Borrower.class))).thenReturn(testBorrower);

            borrowerService.blockBorrower(testBorrower.getId());

            assertThat(testBorrower.getStatus()).isEqualTo(BorrowerStatus.BLOCKED);
            verify(auditService).logStatusChange(eq("Borrower"), eq(testBorrower.getId()), any(), any());
        }
    }

    @Nested
    @DisplayName("deleteBorrower")
    class DeleteBorrowerTests {

        @Test
        @DisplayName("Should soft delete borrower")
        void shouldSoftDeleteBorrower() {
            when(securityUtils.getCurrentUserId()).thenReturn(currentUser.getId());
            when(borrowerRepository.findByIdAndCreatedByAndNotDeleted(testBorrower.getId(), currentUser.getId()))
                    .thenReturn(Optional.of(testBorrower));
            when(borrowerRepository.save(any(Borrower.class))).thenReturn(testBorrower);

            borrowerService.deleteBorrower(testBorrower.getId());

            assertThat(testBorrower.getDeletedAt()).isNotNull();
            verify(auditService).logDelete(eq("Borrower"), eq(testBorrower.getId()), any());
        }
    }
}
