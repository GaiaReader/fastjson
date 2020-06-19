package com.alibaba.fastjson.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.annotation.JSONCreator;
import com.alibaba.fastjson.annotation.JSONField;

public class DeserializeBeanInfo {

    private final Class<?>        clazz;
    private final Type            type;
    private Constructor<?>        defaultConstructor;
    private Constructor<?>        creatorConstructor;
    private Method                factoryMethod;

    private final List<FieldInfo> fieldList = new ArrayList<FieldInfo>();

    public DeserializeBeanInfo(Class<?> clazz){
        super();
        this.clazz = clazz;
        this.type = clazz;
    }

    public Constructor<?> getDefaultConstructor() {
        return defaultConstructor;
    }

    public void setDefaultConstructor(Constructor<?> defaultConstructor) {
        this.defaultConstructor = defaultConstructor;
    }

    public Constructor<?> getCreatorConstructor() {
        return creatorConstructor;
    }

    public void setCreatorConstructor(Constructor<?> createConstructor) {
        this.creatorConstructor = createConstructor;
    }

    public Method getFactoryMethod() {
        return factoryMethod;
    }

    public void setFactoryMethod(Method factoryMethod) {
        this.factoryMethod = factoryMethod;
    }

    public Class<?> getClazz() {
        return clazz;
    }

    public Type getType() {
        return type;
    }

    public List<FieldInfo> getFieldList() {
        return fieldList;
    }
    
    public boolean add(FieldInfo field) {
        for (FieldInfo item : this.fieldList) {
            if (item.getName().equals(field.getName())) {
                return false;
            }
        }
        fieldList.add(field);
        
        return true;
    }

    public static DeserializeBeanInfo computeSetters(Class<?> clazz) {// 计算setter
        DeserializeBeanInfo beanInfo = new DeserializeBeanInfo(clazz);

        Constructor<?> defaultConstructor = getDefaultConstructor(clazz);// 默认构造函数
        if (defaultConstructor != null) {
            defaultConstructor.setAccessible(true);// 如果找到，无论是否accessible
            beanInfo.setDefaultConstructor(defaultConstructor);
        } else if (defaultConstructor == null && !(clazz.isInterface() || Modifier.isAbstract(clazz.getModifiers()))) {// 如果找不到默认构造函数，且非接口非抽象
            Constructor<?> creatorConstructor = getCreatorConstructor(clazz);// fastjson可以通过@JSONCreator注解来标识，这一部分需要手动指定，不在分析范围内
            if (creatorConstructor != null) {
                creatorConstructor.setAccessible(true);
                beanInfo.setCreatorConstructor(creatorConstructor);

                for (int i = 0; i < creatorConstructor.getParameterTypes().length; ++i) {
                    Annotation[] paramAnnotations = creatorConstructor.getParameterAnnotations()[i];
                    JSONField fieldAnnotation = null;
                    for (Annotation paramAnnotation : paramAnnotations) {
                        if (paramAnnotation instanceof JSONField) {
                            fieldAnnotation = (JSONField) paramAnnotation;
                            break;
                        }
                    }
                    if (fieldAnnotation == null) {
                        throw new JSONException("illegal json creator");
                    }

                    Class<?> fieldClass = creatorConstructor.getParameterTypes()[i];
                    Type fieldType = creatorConstructor.getGenericParameterTypes()[i];
                    Field field = getField(clazz, fieldAnnotation.name());
                    if (field != null) {
                        field.setAccessible(true);
                    }
                    FieldInfo fieldInfo = new FieldInfo(fieldAnnotation.name(), clazz, fieldClass, fieldType, null,
                                                        field);
                    beanInfo.add(fieldInfo);
                }
                return beanInfo;
            }
            // OK，没有使用JSONCreator注解，默认到达这里
            Method factoryMethod = getFactoryMethod(clazz);// 获取工厂方法，需要手动在工厂方法中添加@JSONCreator注解
            if (factoryMethod != null) {
                factoryMethod.setAccessible(true);
                beanInfo.setFactoryMethod(factoryMethod);

                for (int i = 0; i < factoryMethod.getParameterTypes().length; ++i) {
                    Annotation[] paramAnnotations = factoryMethod.getParameterAnnotations()[i];
                    JSONField fieldAnnotation = null;
                    for (Annotation paramAnnotation : paramAnnotations) {
                        if (paramAnnotation instanceof JSONField) {
                            fieldAnnotation = (JSONField) paramAnnotation;
                            break;
                        }
                    }
                    if (fieldAnnotation == null) {
                        throw new JSONException("illegal json creator");
                    }

                    Class<?> fieldClass = factoryMethod.getParameterTypes()[i];
                    Type fieldType = factoryMethod.getGenericParameterTypes()[i];
                    Field field = getField(clazz, fieldAnnotation.name());
                    if (field != null) {
                        field.setAccessible(true);
                    }
                    FieldInfo fieldInfo = new FieldInfo(fieldAnnotation.name(), clazz, fieldClass, fieldType, null,
                                                        field);
                    beanInfo.add(fieldInfo);
                }
                return beanInfo;
            }
            
            throw new JSONException("default constructor not found. " + clazz);// 所以，没有默认构造方法，非接口非抽象，是抛出异常的，
        }

        for (Method method : clazz.getMethods()) {// public 方法
            String methodName = method.getName();
            if (methodName.length() < 4) {
                continue;
            }

            if (Modifier.isStatic(method.getModifiers())) {
                continue;
            }

            // support builder set
            if (!(method.getReturnType().equals(Void.TYPE) || method.getReturnType().equals(clazz))) {// 仅允许返回值为void或当前类
                continue;
            }

            if (method.getParameterTypes().length != 1) {// 入参数量必须为1
                continue;
            }

            JSONField annotation = method.getAnnotation(JSONField.class);

            if (annotation != null) {// JSONField标记，非默认，不分析
                if (!annotation.deserialize()) {
                    continue;
                }

                if (annotation.name().length() != 0) {
                    String propertyName = annotation.name();
                    beanInfo.add(new FieldInfo(propertyName, method, null));
                    method.setAccessible(true);
                    continue;
                }
            }

            if (methodName.startsWith("set") && Character.isUpperCase(methodName.charAt(3))) {// 形如setAbc，则属性名为abc
                String propertyName = Character.toLowerCase(methodName.charAt(3)) + methodName.substring(4);

                Field field = getField(clazz, propertyName);
                if (field != null) {

                    JSONField fieldAnnotation = field.getAnnotation(JSONField.class);// 如果有指定JSONField，则beanInfo添加fieldinfo与field进行绑定

                    if (fieldAnnotation != null && fieldAnnotation.name().length() != 0) {
                        propertyName = fieldAnnotation.name();

                        beanInfo.add(new FieldInfo(propertyName, method, field));
                        continue;
                    }
                }

                beanInfo.add(new FieldInfo(propertyName, method, null));
                method.setAccessible(true);// 奇怪，getMethods应该就已经是public方法了吧，这里再setAccessible为了什么
            }
        }

        for (Field field : clazz.getFields()) {// 遍历field，all accessible public fields
            if (Modifier.isStatic(field.getModifiers())) {// 非static
                continue;
            }

            if (!Modifier.isPublic(field.getModifiers())) {// 这个还有不满足的么？
                continue;
            }
            
            boolean contains = false;
            for (FieldInfo item : beanInfo.getFieldList()) {
                if (item.getName().equals(field.getName())) {
                    contains = true;
                    continue;
                }
            }
            
            if (contains) {
                continue;
            }

            beanInfo.add(new FieldInfo(field.getName(), null, field));// 如果前面没有添加，则加一个mthod=null的field
        }

        return beanInfo;
    }

    public static Field getField(Class<?> clazz, String fieldName) {
        try {
            return clazz.getDeclaredField(fieldName);
        } catch (Exception e) {
            return null;
        }
    }

    public static Constructor<?> getDefaultConstructor(Class<?> clazz) {
        if (Modifier.isAbstract(clazz.getModifiers())) {
            return null;
        }

        Constructor<?> defaultConstructor = null;
        for (Constructor<?> constructor : clazz.getDeclaredConstructors()) {// 无参构造函数
            if (constructor.getParameterTypes().length == 0) {
                defaultConstructor = constructor;
                break;
            }
        }
        
        if (defaultConstructor == null) {
            if (clazz.isMemberClass() && !Modifier.isStatic(clazz.getModifiers())) {// 类是成员类，且非静态类（成员类：作为类的成员存在于某个类的内部，静态类：作为类的静态成员存在于某个类的内部）
                for (Constructor<?> constructor : clazz.getDeclaredConstructors()) {// 对于这种情况，允许参数为当前类的构造方式
                    if (constructor.getParameterTypes().length == 1 && constructor.getParameterTypes()[0].equals(clazz.getDeclaringClass())) {
                        defaultConstructor = constructor;
                        break;
                    }
                }
            }
        }
        
        return defaultConstructor;
    }

    public static Constructor<?> getCreatorConstructor(Class<?> clazz) {
        Constructor<?> creatorConstructor = null;

        for (Constructor<?> constructor : clazz.getDeclaredConstructors()) {
            JSONCreator annotation = constructor.getAnnotation(JSONCreator.class);
            if (annotation != null) {
                if (creatorConstructor != null) {
                    throw new JSONException("multi-json creator");
                }

                creatorConstructor = constructor;
                break;
            }
        }
        return creatorConstructor;
    }

    public static Method getFactoryMethod(Class<?> clazz) {
        Method factoryMethod = null;

        for (Method method : clazz.getDeclaredMethods()) {
            if (!Modifier.isStatic(method.getModifiers())) {
                continue;
            }

            if (!clazz.isAssignableFrom(method.getReturnType())) {
                continue;
            }

            JSONCreator annotation = method.getAnnotation(JSONCreator.class);
            if (annotation != null) {
                if (factoryMethod != null) {
                    throw new JSONException("multi-json creator");
                }

                factoryMethod = method;
                break;
            }
        }
        return factoryMethod;
    }

}
