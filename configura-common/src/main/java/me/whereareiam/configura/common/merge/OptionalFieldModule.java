package me.whereareiam.configura.common.merge;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import me.whereareiam.configura.annotation.Field;

import java.util.ArrayList;
import java.util.List;

/**
 * Jackson module that handles serialization of @Field(optional=true) fields.
 * Forces optional fields to be written as explicit null instead of being omitted.
 */
public class OptionalFieldModule extends SimpleModule {
	public OptionalFieldModule() {
		super("OptionalFieldModule");
		setSerializerModifier(new OptionalFieldSerializerModifier());
	}

	private static class OptionalFieldSerializerModifier extends BeanSerializerModifier {
		@Override
		public List<BeanPropertyWriter> changeProperties(
				SerializationConfig config,
				BeanDescription beanDesc,
				List<BeanPropertyWriter> beanProperties) {

			List<BeanPropertyWriter> newWriters = new ArrayList<>();

			for (BeanPropertyWriter writer : beanProperties) {
				if (isOptionalField(writer)) {
					newWriters.add(new OptionalPropertyWriter(writer));
					continue;
				}

				newWriters.add(writer);
			}

			return newWriters;
		}

		private boolean isOptionalField(BeanPropertyWriter writer) {
			try {
				Field fieldAnnotation = writer.getMember().getAnnotation(Field.class);
				return fieldAnnotation != null && fieldAnnotation.optional();
			} catch (Exception e) {
				return false;
			}
		}
	}

	private static class OptionalPropertyWriter extends BeanPropertyWriter {
		public OptionalPropertyWriter(BeanPropertyWriter base) {
			super(base);
		}

		@Override
		public void serializeAsField(Object bean, JsonGenerator gen, SerializerProvider prov) throws Exception {
			Object value = get(bean);

			// Always write @Field(optional=true) fields, even if null
			if (value == null) {
				gen.writeFieldName(_name);
				gen.writeNull();
			} else {
				super.serializeAsField(bean, gen, prov);
			}
		}
	}
}
