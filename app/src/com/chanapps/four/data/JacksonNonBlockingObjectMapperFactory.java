package com.chanapps.four.data;

import com.android.gallery3d.ui.Log;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.JsonToken;
import org.codehaus.jackson.Version;
import org.codehaus.jackson.map.*;
import org.codehaus.jackson.map.deser.*;
import org.codehaus.jackson.map.introspect.BasicBeanDescription;
import org.codehaus.jackson.map.module.SimpleDeserializers;
import org.codehaus.jackson.map.module.SimpleModule;
import org.codehaus.jackson.map.module.SimpleSerializers;
import org.codehaus.jackson.type.JavaType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: johnarleyburns
 * Date: 2/25/13
 * Time: 12:15 PM
 * To change this template use File | Settings | File Templates.
 */
public class JacksonNonBlockingObjectMapperFactory {

    public JacksonNonBlockingObjectMapperFactory() {}

    public static class NonBlockingIntegerDeserializer extends JsonDeserializer<Integer> {
        private JsonDeserializer<?> delegate;
        public NonBlockingIntegerDeserializer() {
            this.delegate = new StdDeserializer.IntegerDeserializer(Integer.class, 0);
        }
        @Override
        public Integer deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
            Object o = null;
            try {
                if (delegate != null)
                    o = delegate.deserialize(jp, ctxt);
            }
            catch (JsonMappingException e) {
                try {
                    boolean b = jp.getBooleanValue();
                    o = b ? 1 : 0;
                }
                catch (Exception e2) {
                    if (delegate != null)
                        o = delegate.getNullValue();
                }
            }
            catch (Exception e) {
                if (delegate != null)
                    o = delegate.getNullValue();
            }
            return (Integer)o;
        }
    }

    public static class NonBlockingLongDeserializer extends JsonDeserializer<Long> {
        private JsonDeserializer<?> delegate;
        public NonBlockingLongDeserializer() {
            this.delegate = new StdDeserializer.LongDeserializer(Long.class, 0L);
        }
        @Override
        public Long deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
            Object o = null;
            try {
                if (delegate != null)
                    o = delegate.deserialize(jp, ctxt);
            }
            catch (Exception e) {
                if (delegate != null)
                    o = delegate.getNullValue();
            }
            return (Long)o;
        }
    }

    public static class NonBlockingBooleanDeserializer extends JsonDeserializer<Boolean> {
        private JsonDeserializer<?> delegate;
        public NonBlockingBooleanDeserializer() {
            this.delegate = new StdDeserializer.BooleanDeserializer(Boolean.class, false);
        }
        @Override
        public Boolean deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
            Object o = null;
            try {
                if (delegate != null)
                    o = delegate.deserialize(jp, ctxt);
            }
            catch (Exception e) {
                if (delegate != null)
                    o = delegate.getNullValue();
            }
            return (Boolean)o;
        }
    }

    public static class NonBlockingStringDeserializer extends JsonDeserializer<String> {
        private JsonDeserializer<?> delegate;
        public NonBlockingStringDeserializer() {
            this.delegate = new StdDeserializer.StringDeserializer();
        }
        @Override
        public String deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
            Object o = null;
            try {
                if (delegate != null)
                    o = delegate.deserialize(jp, ctxt);
            }
            catch (Exception e) {
                if (delegate != null)
                    o = delegate.getNullValue();
            }
            return (String)o;
        }
    }

    public static class NonBlockingDateDeserializer extends JsonDeserializer<Date> {
        private JsonDeserializer<?> delegate;
        public NonBlockingDateDeserializer() {
            this.delegate = new StdDeserializer.CalendarDeserializer();
        }
        @Override
        public Date deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
            Object o = null;
            try {
                if (delegate != null)
                    o = delegate.deserialize(jp, ctxt);
            }
            catch (Exception e) {
                if (delegate != null)
                    o = delegate.getNullValue();
            }
            return null;
        }
    }

    public ObjectMapper createObjectMapper(){
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper;
    }

}
