package com.verdissia.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

@Service
@Slf4j
public class TemplateService {

    public String loadTemplate(String templatePath) {
        try {
            Reader reader = new InputStreamReader(
                getClass().getResourceAsStream(templatePath), 
                StandardCharsets.UTF_8
            );
            return FileCopyUtils.copyToString(reader);
        } catch (IOException e) {
            log.error("Failed to load template: {}", templatePath, e);
            throw new RuntimeException("Template not found: " + templatePath, e);
        }
    }

    public String processTemplate(String templateContent, TemplateVariables variables) {
        String result = templateContent;
        
        for (TemplateVariable variable : variables.getVariables()) {
            result = result.replace("{{" + variable.getKey() + "}}", variable.getValue());
        }
        
        return result;
    }

    public static class TemplateVariables {
        private final java.util.List<TemplateVariable> variables = new java.util.ArrayList<>();

        public TemplateVariables add(String key, String value) {
            variables.add(new TemplateVariable(key, value));
            return this;
        }

        public java.util.List<TemplateVariable> getVariables() {
            return variables;
        }
    }

    private static class TemplateVariable {
        private final String key;
        private final String value;

        public TemplateVariable(String key, String value) {
            this.key = key;
            this.value = value;
        }

        public String getKey() {
            return key;
        }

        public String getValue() {
            return value;
        }
    }
}
