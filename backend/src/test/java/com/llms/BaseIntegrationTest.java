package com.llms;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.llms.config.TestConfig;
import com.llms.entity.User;
import com.llms.enums.UserRole;
import com.llms.enums.UserStatus;
import com.llms.repository.*;
import com.llms.security.JwtTokenProvider;
import com.llms.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestConfig.class)
@Transactional
public abstract class BaseIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected BorrowerRepository borrowerRepository;

    @Autowired
    protected LoanRepository loanRepository;

    @Autowired
    protected RepaymentScheduleRepository scheduleRepository;

    @Autowired
    protected PaymentRepository paymentRepository;

    @Autowired
    protected PenaltyRepository penaltyRepository;

    @Autowired
    protected AuditLogRepository auditLogRepository;

    @Autowired
    protected JwtTokenProvider jwtTokenProvider;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    protected User lenderUser;
    protected User adminUser;
    protected User auditorUser;
    protected String lenderToken;
    protected String adminToken;
    protected String auditorToken;

    @BeforeEach
    void setUpBase() {
        // Create test users
        lenderUser = createUser("lender@test.com", "Lender User", UserRole.LENDER);
        adminUser = createUser("admin@test.com", "Admin User", UserRole.ADMIN);
        auditorUser = createUser("auditor@test.com", "Auditor User", UserRole.AUDITOR);

        // Generate tokens
        lenderToken = generateToken(lenderUser);
        adminToken = generateToken(adminUser);
        auditorToken = generateToken(auditorUser);
    }

    protected User createUser(String email, String name, UserRole role) {
        User user = User.builder()
                .email(email)
                .name(name)
                .passwordHash(passwordEncoder.encode("password123"))
                .role(role)
                .status(UserStatus.ACTIVE)
                .build();
        return userRepository.save(user);
    }

    protected String generateToken(User user) {
        UserPrincipal principal = new UserPrincipal(user);
        return jwtTokenProvider.generateToken(principal);
    }

    protected String asJsonString(Object obj) throws Exception {
        return objectMapper.writeValueAsString(obj);
    }
}
