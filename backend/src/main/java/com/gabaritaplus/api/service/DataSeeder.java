package com.gabaritaplus.api.service;

import com.gabaritaplus.api.dto.question.AlternativeRequest;
import com.gabaritaplus.api.entity.Role;
import com.gabaritaplus.api.entity.User;
import com.gabaritaplus.api.entity.enums.DifficultyLevel;
import com.gabaritaplus.api.entity.enums.RoleName;
import com.gabaritaplus.api.repository.QuestionRepository;
import com.gabaritaplus.api.repository.RoleRepository;
import com.gabaritaplus.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.seed", name = "enabled", havingValue = "true")
public class DataSeeder implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final QuestionRepository questionRepository;

    @Override
    public void run(String... args) {
        log.info("Seed controlado habilitado. Verificando dados iniciais.");
        Role userRole = roleRepository.findByName(RoleName.ROLE_USER).orElseGet(() -> createRole(RoleName.ROLE_USER, "UsuÃ¡rio padrÃ£o"));
        Role adminRole = roleRepository.findByName(RoleName.ROLE_ADMIN).orElseGet(() -> createRole(RoleName.ROLE_ADMIN, "Administrador da plataforma"));

        if (!userRepository.existsByEmail("admin@gabaritaplus.com")) {
            User admin = new User();
            admin.setFullName("Admin Gabarita+");
            admin.setEmail("admin@gabaritaplus.com");
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("Admin@123"));
            admin.setRoles(Set.of(adminRole, userRole));
            userRepository.save(admin);
            log.info("Usuario seed admin criado.");
        }

        if (!userRepository.existsByEmail("user@gabaritaplus.com")) {
            User user = new User();
            user.setFullName("Aluno Demo");
            user.setEmail("user@gabaritaplus.com");
            user.setUsername("aluno");
            user.setPassword(passwordEncoder.encode("User@123"));
            user.setRoles(Set.of(userRole));
            userRepository.save(user);
            log.info("Usuario seed demo criado.");
        }

        if (questionRepository.count() == 0) {
            createSampleQuestions();
            log.info("Questoes seed criadas com sucesso.");
        }
    }

    private Role createRole(RoleName roleName, String description) {
        Role role = new Role();
        role.setName(roleName);
        role.setDescription(description);
        return roleRepository.save(role);
    }

    private void createSampleQuestions() {
        questionRepository.saveAll(List.of(
                buildQuestion(
                        "InterpretaÃ§Ã£o textual ENEM",
                        "O texto discute os impactos da urbanizaÃ§Ã£o acelerada. Assinale a alternativa que apresenta a ideia central.",
                        "Linguagens",
                        "InterpretaÃ§Ã£o de Texto",
                        "CompreensÃ£o textual",
                        DifficultyLevel.MEDIUM,
                        2023,
                        "ENEM PPL",
                        "CompetÃªncia 1",
                        "Habilidade 4",
                        "A ideia central aborda efeitos sociais e ambientais da urbanizaÃ§Ã£o.",
                        "B",
                        List.of(
                                new AlternativeRequest("A", "A urbanizaÃ§Ã£o elimina desigualdades histÃ³ricas.", false),
                                new AlternativeRequest("B", "A urbanizaÃ§Ã£o pode gerar impactos sociais e ambientais relevantes.", true),
                                new AlternativeRequest("C", "O texto defende exclusivamente o crescimento industrial.", false),
                                new AlternativeRequest("D", "O foco principal estÃ¡ na produÃ§Ã£o agrÃ­cola.", false),
                                new AlternativeRequest("E", "A crÃ­tica central Ã© sobre tecnologia domÃ©stica.", false)
                        )
                ),
                buildQuestion(
                        "FunÃ§Ã£o do segundo grau",
                        "Uma parÃ¡bola possui raÃ­zes 2 e 6. Qual Ã© o eixo de simetria da funÃ§Ã£o?",
                        "MatemÃ¡tica",
                        "FunÃ§Ãµes",
                        "FunÃ§Ã£o quadrÃ¡tica",
                        DifficultyLevel.EASY,
                        2022,
                        "ENEM Regular",
                        "CompetÃªncia 5",
                        "Habilidade 21",
                        "O eixo de simetria Ã© a mÃ©dia aritmÃ©tica entre as raÃ­zes.",
                        "C",
                        List.of(
                                new AlternativeRequest("A", "x = 2", false),
                                new AlternativeRequest("B", "x = 3", false),
                                new AlternativeRequest("C", "x = 4", true),
                                new AlternativeRequest("D", "x = 5", false),
                                new AlternativeRequest("E", "x = 6", false)
                        )
                )
        ));
    }

    private com.gabaritaplus.api.entity.Question buildQuestion(String title,
                                                               String statement,
                                                               String subject,
                                                               String topic,
                                                               String subtopic,
                                                               DifficultyLevel difficulty,
                                                               int year,
                                                               String exam,
                                                               String competency,
                                                               String ability,
                                                               String explanation,
                                                               String correctAlternative,
                                                               List<AlternativeRequest> alternatives) {
        com.gabaritaplus.api.entity.Question question = new com.gabaritaplus.api.entity.Question();
        question.setTitle(title);
        question.setStatement(statement);
        question.setSubject(subject);
        question.setTopic(topic);
        question.setSubtopic(subtopic);
        question.setDifficulty(difficulty);
        question.setYear(year);
        question.setExam(exam);
        question.setCompetency(competency);
        question.setAbility(ability);
        question.setExplanation(explanation);
        question.setCorrectAlternative(correctAlternative);
        alternatives.forEach(item -> {
            com.gabaritaplus.api.entity.Alternative alternative = new com.gabaritaplus.api.entity.Alternative();
            alternative.setLetter(item.letter());
            alternative.setText(item.text());
            alternative.setCorrect(item.correct());
            alternative.setQuestion(question);
            question.getAlternatives().add(alternative);
        });
        return question;
    }
}
