package ct01.n07.backend.architecture;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.RegexPatternTypeFilter;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ServiceDependencyArchitectureTest {

    private static final String SERVICE_IMPL_PACKAGE = "ct01.n07.backend.service.impl";
    private static final String SERVICE_PACKAGE_PREFIX = "ct01.n07.backend.service";

    @Test
    void serviceImplementations_shouldNotInjectOtherServices() throws ClassNotFoundException {
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new RegexPatternTypeFilter(Pattern.compile(".*ServiceImpl")));

        List<String> violations = new ArrayList<>();

        for (var beanDef : scanner.findCandidateComponents(SERVICE_IMPL_PACKAGE)) {
            Class<?> clazz = Class.forName(beanDef.getBeanClassName());
            for (Field field : clazz.getDeclaredFields()) {
                Class<?> fieldType = field.getType();
                Package fieldPackage = fieldType.getPackage();
                String packageName = fieldPackage == null ? "" : fieldPackage.getName();

                if (packageName.startsWith(SERVICE_PACKAGE_PREFIX)) {
                    violations.add(clazz.getSimpleName() + "." + field.getName() + " -> " + fieldType.getName());
                }
            }
        }

        assertTrue(violations.isEmpty(),
                "Service implementations must not inject other services. Violations: " + violations);
    }
}

