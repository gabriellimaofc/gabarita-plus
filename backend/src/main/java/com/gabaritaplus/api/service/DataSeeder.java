package com.gabaritaplus.api.service;

import com.gabaritaplus.api.dto.question.AlternativeRequest;
import com.gabaritaplus.api.dto.question.QuestionRequest;
import com.gabaritaplus.api.entity.Role;
import com.gabaritaplus.api.entity.User;
import com.gabaritaplus.api.entity.enums.DifficultyLevel;
import com.gabaritaplus.api.entity.enums.RoleName;
import com.gabaritaplus.api.repository.QuestionRepository;
import com.gabaritaplus.api.repository.RoleRepository;
import com.gabaritaplus.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final QuestionRepository questionRepository;

    @Override
    public void run(String... args) {
        Role userRole = roleRepository.findByName(RoleName.ROLE_USER).orElseGet(() -> createRole(RoleName.ROLE_USER, "Usuário padrão"));
        Role adminRole = roleRepository.findByName(RoleName.ROLE_ADMIN).orElseGet(() -> createRole(RoleName.ROLE_ADMIN, "Administrador da plataforma"));

        if (!userRepository.existsByEmail("admin@gabaritaplus.com")) {
            User admin = new User();
            admin.setFullName("Admin Gabarita+");
            admin.setEmail("admin@gabaritaplus.com");
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("Admin@123"));
            admin.setRoles(Set.of(adminRole, userRole));
            userRepository.save(admin);
        }

        if (!userRepository.existsByEmail("user@gabaritaplus.com")) {
            User user = new User();
            user.setFullName("Aluno Demo");
            user.setEmail("user@gabaritaplus.com");
            user.setUsername("aluno");
            user.setPassword(passwordEncoder.encode("User@123"));
            user.setRoles(Set.of(userRole));
            userRepository.save(user);
        }

        if (questionRepository.count() == 0) {
            createSampleQuestions();
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
                        "Interpretação textual ENEM",
                        "O texto discute os impactos da urbanização acelerada. Assinale a alternativa que apresenta a ideia central.",
                        "Linguagens",
                        "Interpretação de Texto",
                        "Compreensão textual",
                        DifficultyLevel.MEDIUM,
                        2023,
                        "ENEM PPL",
                        "Competência 1",
                        "Habilidade 4",
                        "A ideia central aborda efeitos sociais e ambientais da urbanização.",
                        "B",
                        List.of(
                                new AlternativeRequest("A", "A urbanização elimina desigualdades históricas.", false),
                                new AlternativeRequest("B", "A urbanização pode gerar impactos sociais e ambientais relevantes.", true),
                                new AlternativeRequest("C", "O texto defende exclusivamente o crescimento industrial.", false),
                                new AlternativeRequest("D", "O foco principal está na produção agrícola.", false),
                                new AlternativeRequest("E", "A crítica central é sobre tecnologia doméstica.", false)
                        )
                ),
                buildQuestion(
                        "Função do segundo grau",
                        "Uma parábola possui raízes 2 e 6. Qual é o eixo de simetria da função?",
                        "Matemática",
                        "Funções",
                        "Função quadrática",
                        DifficultyLevel.EASY,
                        2022,
                        "ENEM Regular",
                        "Competência 5",
                        "Habilidade 21",
                        "O eixo de simetria é a média aritmética entre as raízes.",
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
