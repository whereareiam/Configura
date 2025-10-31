package me.whereareiam.configura.common.template;

import com.fasterxml.jackson.databind.ObjectMapper;
import me.whereareiam.configura.annotation.Field;
import me.whereareiam.configura.annotation.Template;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TemplateSeederTest {
    static class A {
        @Field
        @Template(text = "x")
        public String v;
    }

    @Test
    void appliesTemplateWhenMissing() {
        ObjectMapper mapper = new ObjectMapper();
        A a = new A();
        a.v = null;
        new TemplateSeeder(mapper, null).seed(a);
        assertEquals("x", a.v);
    }
}