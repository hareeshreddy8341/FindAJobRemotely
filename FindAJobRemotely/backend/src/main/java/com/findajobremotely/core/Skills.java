package com.findajobremotely.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Skill-name helpers: canonical forms for matching and cleaning of user-entered lists. */
public final class Skills {

    private static final Map<String, String> ALIASES = new HashMap<>();

    static {
        alias("js", "javascript");
        alias("ecmascript", "javascript");
        alias("ts", "typescript");
        alias("reactjs", "react");
        alias("react.js", "react");
        alias("node", "node.js");
        alias("nodejs", "node.js");
        alias("vuejs", "vue");
        alias("vue.js", "vue");
        alias("next", "next.js");
        alias("nextjs", "next.js");
        alias("golang", "go");
        alias("py", "python");
        alias("postgres", "postgresql");
        alias("mongo", "mongodb");
        alias("k8s", "kubernetes");
        alias("cpp", "c++");
        alias("csharp", "c#");
        alias("c sharp", "c#");
        alias("springboot", "spring boot");
        alias("spring-boot", "spring boot");
        alias("ml", "machine learning");
        alias("tailwindcss", "tailwind");
        alias("tailwind css", "tailwind");
        alias("html5", "html");
        alias("css3", "css");
        alias("amazon web services", "aws");
        alias("gcp", "google cloud");
    }

    private Skills() {
    }

    private static void alias(String alias, String canonical) {
        ALIASES.put(alias, canonical);
    }

    /** Lower-cases, collapses whitespace and resolves well-known aliases ("ReactJS" and "react" both give "react"). */
    public static String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        String s = raw.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
        return ALIASES.getOrDefault(s, s);
    }

    public static Set<String> normalizeAll(Collection<String> raw) {
        Set<String> out = new LinkedHashSet<>();
        if (raw == null) {
            return out;
        }
        for (String r : raw) {
            String n = normalize(r);
            if (!n.isEmpty()) {
                out.add(n);
            }
        }
        return out;
    }

    /** Cleans a user-entered list: sanitises, drops blanks, de-duplicates by canonical form and caps the size. */
    public static List<String> clean(Collection<String> raw, int maxItems) {
        Map<String, String> byKey = new LinkedHashMap<>();
        if (raw != null) {
            for (String r : raw) {
                String c = TextClean.line(r);
                if (c == null || c.isEmpty()) {
                    continue;
                }
                byKey.putIfAbsent(normalize(c), c);
                if (byKey.size() >= maxItems) {
                    break;
                }
            }
        }
        return new ArrayList<>(byKey.values());
    }
}
