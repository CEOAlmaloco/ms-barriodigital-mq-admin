package cl.duoc.barriodigital.mqadmin.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "barriodigital.mq")
public class MqProtectionProperties {

    private List<String> protectedNames = new ArrayList<>();
    private List<String> protectedPrefixes = new ArrayList<>();

    public boolean isProtected(String name) {
        if (name == null || name.isBlank()) {
            return true;
        }
        for (String exact : protectedNames) {
            if (name.equals(exact)) {
                return true;
            }
        }
        for (String prefix : protectedPrefixes) {
            if (prefix != null && !prefix.isEmpty() && name.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    public List<String> getProtectedNames() {
        return protectedNames;
    }

    public void setProtectedNames(List<String> protectedNames) {
        this.protectedNames = protectedNames == null ? new ArrayList<>() : protectedNames;
    }

    public List<String> getProtectedPrefixes() {
        return protectedPrefixes;
    }

    public void setProtectedPrefixes(List<String> protectedPrefixes) {
        this.protectedPrefixes = protectedPrefixes == null ? new ArrayList<>() : protectedPrefixes;
    }
}
