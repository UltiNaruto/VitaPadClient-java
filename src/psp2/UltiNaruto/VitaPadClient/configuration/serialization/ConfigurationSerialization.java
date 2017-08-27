package psp2.UltiNaruto.VitaPadClient.configuration.serialization;

/*     */ import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.Validate;
/*     */ 
/*     */ public class ConfigurationSerialization
/*     */ {
/*     */   public static final String SERIALIZED_TYPE_KEY = "==";
/*     */   private final Class<? extends ConfigurationSerializable> clazz;
/*  27 */   private static Map<String, Class<? extends ConfigurationSerializable>> aliases = new HashMap();
/*     */ 
/*     */   protected ConfigurationSerialization(Class<? extends ConfigurationSerializable> clazz)
/*     */   {
/*  39 */     this.clazz = clazz;
/*     */   }
/*     */ 
/*     */   protected Method getMethod(String name, boolean isStatic) {
/*     */     try {
/*  44 */       Method method = this.clazz.getDeclaredMethod(name, new Class[] { Map.class });
/*     */ 
/*  46 */       if (!ConfigurationSerializable.class.isAssignableFrom(method.getReturnType())) {
/*  47 */         return null;
/*     */       }
/*  49 */       if (Modifier.isStatic(method.getModifiers()) != isStatic) {
/*  50 */         return null;
/*     */       }
/*     */ 
/*  53 */       return method;
/*     */     } catch (NoSuchMethodException ex) {
/*  55 */       return null; } catch (SecurityException ex) {
/*     */     }
/*  57 */     return null;
/*     */   }
/*     */ 
/*     */   protected Constructor<? extends ConfigurationSerializable> getConstructor()
/*     */   {
/*     */     try {
/*  63 */       return this.clazz.getConstructor(new Class[] { Map.class });
/*     */     } catch (NoSuchMethodException ex) {
/*  65 */       return null; } catch (SecurityException ex) {
/*     */     }
/*  67 */     return null;
/*     */   }
/*     */ 
/*     */   protected ConfigurationSerializable deserializeViaMethod(Method method, Map<String, ?> args)
/*     */   {
/*     */     try {
/*  73 */       ConfigurationSerializable result = (ConfigurationSerializable)method.invoke(null, new Object[] { args });
/*     */ 
/*  75 */       if (result == null)
/*  76 */         Logger.getLogger(ConfigurationSerialization.class.getName()).log(Level.SEVERE, "Could not call method '" + method.toString() + "' of " + this.clazz + " for deserialization: method returned null");
/*     */       else
/*  78 */         return result;
/*     */     }
/*     */     catch (Throwable ex) {
/*  81 */       Logger.getLogger(ConfigurationSerialization.class.getName()).log(Level.SEVERE, "Could not call method '" + method.toString() + "' of " + this.clazz + " for deserialization", (ex instanceof InvocationTargetException) ? ex.getCause() : ex);
/*     */     }
/*     */ 
/*  87 */     return null;
/*     */   }
/*     */ 
/*     */   protected ConfigurationSerializable deserializeViaCtor(Constructor<? extends ConfigurationSerializable> ctor, Map<String, ?> args) {
/*     */     try {
/*  92 */       return (ConfigurationSerializable)ctor.newInstance(new Object[] { args });
/*     */     } catch (Throwable ex) {
/*  94 */       Logger.getLogger(ConfigurationSerialization.class.getName()).log(Level.SEVERE, "Could not call constructor '" + ctor.toString() + "' of " + this.clazz + " for deserialization", (ex instanceof InvocationTargetException) ? ex.getCause() : ex);
/*     */     }
/*     */ 
/* 100 */     return null;
/*     */   }
/*     */ 
/*     */   public ConfigurationSerializable deserialize(Map<String, ?> args) {
/* 104 */     Validate.notNull(args, "Args must not be null");
/*     */ 
/* 106 */     ConfigurationSerializable result = null;
/* 107 */     Method method = null;
/*     */ 
/* 109 */     if (result == null) {
/* 110 */       method = getMethod("deserialize", true);
/*     */ 
/* 112 */       if (method != null) {
/* 113 */         result = deserializeViaMethod(method, args);
/*     */       }
/*     */     }
/*     */ 
/* 117 */     if (result == null) {
/* 118 */       method = getMethod("valueOf", true);
/*     */ 
/* 120 */       if (method != null) {
/* 121 */         result = deserializeViaMethod(method, args);
/*     */       }
/*     */     }
/*     */ 
/* 125 */     if (result == null) {
/* 126 */       Constructor constructor = getConstructor();
/*     */ 
/* 128 */       if (constructor != null) {
/* 129 */         result = deserializeViaCtor(constructor, args);
/*     */       }
/*     */     }
/*     */ 
/* 133 */     return result;
/*     */   }
/*     */ 
/*     */   public static ConfigurationSerializable deserializeObject(Map<String, ?> args, Class<? extends ConfigurationSerializable> clazz)
/*     */   {
/* 150 */     return new ConfigurationSerialization(clazz).deserialize(args);
/*     */   }
/*     */ 
/*     */   public static ConfigurationSerializable deserializeObject(Map<String, ?> args)
/*     */   {
/* 166 */     Class clazz = null;
/*     */ 
/* 168 */     if (args.containsKey("=="))
/*     */       try {
/* 170 */         String alias = (String)args.get("==");
/*     */ 
/* 172 */         if (alias == null) {
/* 173 */           throw new IllegalArgumentException("Cannot have null alias");
/*     */         }
/* 175 */         clazz = getClassByAlias(alias);
/* 176 */         if (clazz == null)
/* 177 */           throw new IllegalArgumentException("Specified class does not exist ('" + alias + "')");
/*     */       }
/*     */       catch (ClassCastException ex) {
/* 180 */         ex.fillInStackTrace();
/* 181 */         throw ex;
/*     */       }
/*     */     else {
/* 184 */       throw new IllegalArgumentException("Args doesn't contain type key ('==')");
/*     */     }
/*     */ 
/* 187 */     return new ConfigurationSerialization(clazz).deserialize(args);
/*     */   }
/*     */ 
/*     */   public static void registerClass(Class<? extends ConfigurationSerializable> clazz)
/*     */   {
/* 196 */     DelegateDeserialization delegate = (DelegateDeserialization)clazz.getAnnotation(DelegateDeserialization.class);
/*     */ 
/* 198 */     if (delegate == null) {
/* 199 */       registerClass(clazz, getAlias(clazz));
/* 200 */       registerClass(clazz, clazz.getName());
/*     */     }
/*     */   }
/*     */ 
/*     */   public static void registerClass(Class<? extends ConfigurationSerializable> clazz, String alias)
/*     */   {
/* 212 */     aliases.put(alias, clazz);
/*     */   }
/*     */ 
/*     */   public static void unregisterClass(String alias)
/*     */   {
/* 221 */     aliases.remove(alias);
/*     */   }
/*     */ 
/*     */   public static void unregisterClass(Class<? extends ConfigurationSerializable> clazz)
/*     */   {
/* 230 */     while (aliases.values().remove(clazz));
/*     */   }
/*     */ 
/*     */   public static Class<? extends ConfigurationSerializable> getClassByAlias(String alias)
/*     */   {
/* 242 */     return (Class)aliases.get(alias);
/*     */   }
/*     */ 
/*     */   public static String getAlias(Class<? extends ConfigurationSerializable> clazz)
/*     */   {
/* 252 */     DelegateDeserialization delegate = (DelegateDeserialization)clazz.getAnnotation(DelegateDeserialization.class);
/*     */ 
/*     */     if (delegate != null) {
/*     */       if ((delegate.value() == null) || (delegate.value() == clazz))
/*     */         delegate = null;
/*     */       else {
/*     */         return getAlias(delegate.value());
/*     */       }
/*     */     }
/*     */ 
/*     */     if (delegate == null) {
/*     */       SerializableAs alias = (SerializableAs)clazz.getAnnotation(SerializableAs.class);
/*     */ 
/*     */       if ((alias != null) && (alias.value() != null)) {
/*     */         return alias.value();
/*     */       }
/*     */     }
/*     */ 
/*     */     return clazz.getName();
/*     */   }
/*     */ }