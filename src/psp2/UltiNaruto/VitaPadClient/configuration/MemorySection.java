package psp2.UltiNaruto.VitaPadClient.configuration;

/*     */ import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.Validate;
/*     */ 
/*     */ public class MemorySection
/*     */   implements ConfigurationSection
/*     */ {
/*     */   protected final Map<String, Object> map = new LinkedHashMap();
/*     */   private final Configuration root;
/*     */   private final ConfigurationSection parent;
/*     */   private final String path;
/*     */   private final String fullPath;
/*     */ 
/*     */   protected MemorySection()
/*     */   {
/*     */     if (!(this instanceof Configuration)) {
/*     */       throw new IllegalStateException("Cannot construct a root MemorySection when not a Configuration");
/*     */     }
/*     */ 
/*     */     this.path = "";
/*     */     this.fullPath = "";
/*     */     this.parent = null;
/*     */     this.root = ((Configuration)this);
/*     */   }
/*     */ 
/*     */   protected MemorySection(ConfigurationSection parent, String path)
/*     */   {
/*     */     Validate.notNull(parent, "Parent cannot be null");
/*     */     Validate.notNull(path, "Path cannot be null");
/*     */ 
/*     */     this.path = path;
/*     */     this.parent = parent;
/*     */     this.root = parent.getRoot();
/*     */ 
/*     */     Validate.notNull(this.root, "Path cannot be orphaned");
/*     */ 
/*     */     this.fullPath = createPath(parent, path);
/*     */   }
/*     */ 
/*     */   public Set<String> getKeys(boolean deep) {
/*     */     Set result = new LinkedHashSet();
/*     */ 
/*     */     Configuration root = getRoot();
/*     */     if ((root != null) && (root.options().copyDefaults())) {
/*     */       ConfigurationSection defaults = getDefaultSection();
/*     */ 
/*     */       if (defaults != null) {
/*     */         result.addAll(defaults.getKeys(deep));
/*     */       }
/*     */     }
/*     */ 
/*     */     mapChildrenKeys(result, this, deep);
/*     */ 
/*     */     return result;
/*     */   }
/*     */ 
/*     */   public Map<String, Object> getValues(boolean deep) {
/*     */     Map result = new LinkedHashMap();
/*     */ 
/*     */     Configuration root = getRoot();
/*     */     if ((root != null) && (root.options().copyDefaults())) {
/*     */       ConfigurationSection defaults = getDefaultSection();
/*     */ 
/*     */       if (defaults != null) {
/*     */         result.putAll(defaults.getValues(deep));
/*     */       }
/*     */     }
/*     */ 
/*     */     mapChildrenValues(result, this, deep);
/*     */ 
/*     */     return result;
/*     */   }
/*     */ 
/*     */   public boolean contains(String path) {
/*     */     return get(path) != null;
/*     */   }
/*     */ 
/*     */   public boolean isSet(String path) {
/*     */     Configuration root = getRoot();
/*     */     if (root == null) {
/*     */       return false;
/*     */     }
/*     */     if (root.options().copyDefaults()) {
/*     */       return contains(path);
/*     */     }
/*     */     return get(path, null) != null;
/*     */   }
/*     */ 
/*     */   public String getCurrentPath() {
/*     */     return this.fullPath;
/*     */   }
/*     */ 
/*     */   public String getName() {
/*     */     return this.path;
/*     */   }
/*     */ 
/*     */   public Configuration getRoot() {
/*     */     return this.root;
/*     */   }
/*     */ 
/*     */   public ConfigurationSection getParent() {
/*     */     return this.parent;
/*     */   }
/*     */ 
/*     */   public void addDefault(String path, Object value) {
/*     */     Validate.notNull(path, "Path cannot be null");
/*     */ 
/*     */     Configuration root = getRoot();
/*     */     if (root == null) {
/*     */       throw new IllegalStateException("Cannot add default without root");
/*     */     }
/*     */     if (root == this) {
/*     */       throw new UnsupportedOperationException("Unsupported addDefault(String, Object) implementation");
/*     */     }
/*     */     root.addDefault(createPath(this, path), value);
/*     */   }
/*     */ 
/*     */   public ConfigurationSection getDefaultSection() {
/*     */     Configuration root = getRoot();
/*     */     Configuration defaults = root == null ? null : root.getDefaults();
/*     */ 
/* 149 */     if ((defaults != null) && 
/* 150 */       (defaults.isConfigurationSection(getCurrentPath()))) {
/* 151 */       return defaults.getConfigurationSection(getCurrentPath());
/*     */     }
/*     */ 
/* 155 */     return null;
/*     */   }
/*     */ 
/*     */   public void set(String path, Object value) {
/* 159 */     Validate.notEmpty(path, "Cannot set to an empty path");
/*     */ 
/* 161 */     Configuration root = getRoot();
/* 162 */     if (root == null) {
/* 163 */       throw new IllegalStateException("Cannot use section without a root");
/*     */     }
/*     */ 
/* 166 */     char separator = root.options().pathSeparator();
/*     */ 
/* 169 */     int i1 = -1;
/* 170 */     ConfigurationSection section = this;
/*     */     int i2;
/* 171 */     while ((i1 = path.indexOf(separator, i2 = i1 + 1)) != -1) {
/* 172 */       String node = path.substring(i2, i1);
/* 173 */       ConfigurationSection subSection = section.getConfigurationSection(node);
/* 174 */       if (subSection == null)
/* 175 */         section = section.createSection(node);
/*     */       else {
/* 177 */         section = subSection;
/*     */       }
/*     */     }
/*     */ 
/* 181 */     String key = path.substring(i2);
/* 182 */     if (section == this) {
/* 183 */       if (value == null)
/* 184 */         this.map.remove(key);
/*     */       else
/* 186 */         this.map.put(key, value);
/*     */     }
/*     */     else
/* 189 */       section.set(key, value);
/*     */   }
/*     */ 
/*     */   public Object get(String path)
/*     */   {
/* 194 */     return get(path, getDefault(path));
/*     */   }
/*     */ 
/*     */   public Object get(String path, Object def) {
/* 198 */     Validate.notNull(path, "Path cannot be null");
/*     */ 
/* 200 */     if (path.length() == 0) {
/* 201 */       return this;
/*     */     }
/*     */ 
/* 204 */     Configuration root = getRoot();
/* 205 */     if (root == null) {
/* 206 */       throw new IllegalStateException("Cannot access section without a root");
/*     */     }
/*     */ 
/* 209 */     char separator = root.options().pathSeparator();
/*     */ 
/* 212 */     int i1 = -1;
/* 213 */     ConfigurationSection section = this;
/*     */     int i2;
/* 214 */     while ((i1 = path.indexOf(separator, i2 = i1 + 1)) != -1) {
/* 215 */       section = section.getConfigurationSection(path.substring(i2, i1));
/* 216 */       if (section == null) {
/* 217 */         return def;
/*     */       }
/*     */     }
/*     */ 
/* 221 */     String key = path.substring(i2);
/* 222 */     if (section == this) {
/* 223 */       Object result = this.map.get(key);
/* 224 */       return result == null ? def : result;
/*     */     }
/* 226 */     return section.get(key, def);
/*     */   }
/*     */ 
/*     */   public ConfigurationSection createSection(String path) {
/* 230 */     Validate.notEmpty(path, "Cannot create section at empty path");
/* 231 */     Configuration root = getRoot();
/* 232 */     if (root == null) {
/* 233 */       throw new IllegalStateException("Cannot create section without a root");
/*     */     }
/*     */ 
/* 236 */     char separator = root.options().pathSeparator();
/*     */ 
/* 239 */     int i1 = -1;
/* 240 */     ConfigurationSection section = this;
/*     */     int i2;
/* 241 */     while ((i1 = path.indexOf(separator, i2 = i1 + 1)) != -1) {
/* 242 */       String node = path.substring(i2, i1);
/* 243 */       ConfigurationSection subSection = section.getConfigurationSection(node);
/* 244 */       if (subSection == null)
/* 245 */         section = section.createSection(node);
/*     */       else {
/* 247 */         section = subSection;
/*     */       }
/*     */     }
/*     */ 
/* 251 */     String key = path.substring(i2);
/* 252 */     if (section == this) {
/* 253 */       ConfigurationSection result = new MemorySection(this, key);
/* 254 */       this.map.put(key, result);
/* 255 */       return result;
/*     */     }
/* 257 */     return section.createSection(key);
/*     */   }
/*     */ 
/*     */   public ConfigurationSection createSection(String path, Map<?, ?> map) {
/* 261 */     ConfigurationSection section = createSection(path);
/*     */ 
/* 263 */     for (Map.Entry entry : map.entrySet()) {
/* 264 */       if ((entry.getValue() instanceof Map))
/* 265 */         section.createSection(entry.getKey().toString(), (Map)entry.getValue());
/*     */       else {
/* 267 */         section.set(entry.getKey().toString(), entry.getValue());
/*     */       }
/*     */     }
/*     */ 
/* 271 */     return section;
/*     */   }
/*     */ 
/*     */   public String getString(String path)
/*     */   {
/* 276 */     Object def = getDefault(path);
/* 277 */     return getString(path, def != null ? def.toString() : null);
/*     */   }
/*     */ 
/*     */   public String getString(String path, String def) {
/* 281 */     Object val = get(path, def);
/* 282 */     return val != null ? val.toString() : def;
/*     */   }
/*     */ 
/*     */   public boolean isString(String path) {
/* 286 */     Object val = get(path);
/* 287 */     return val instanceof String;
/*     */   }
/*     */ 
/*     */   public int getInt(String path) {
/* 296 */     Integer val = (Integer)get(path);
/* 342 */     return val!=null?val:0;
/*     */   }
/*     */ 
/*     */   public boolean isInt(String path) {
/* 301 */     Object val = get(path);
/* 302 */     return val instanceof Integer;
/*     */   }
/*     */ 
/*     */   public boolean getBoolean(String path) {
/* 306 */     Object def = getDefault(path);
/* 307 */     return getBoolean(path, (def instanceof Boolean) ? ((Boolean)def).booleanValue() : false);
/*     */   }
/*     */ 
/*     */   public boolean getBoolean(String path, boolean def) {
/* 311 */     Object val = get(path, Boolean.valueOf(def));
/* 312 */     return (val instanceof Boolean) ? ((Boolean)val).booleanValue() : def;
/*     */   }
/*     */ 
/*     */   public boolean isBoolean(String path) {
/* 316 */     Object val = get(path);
/* 317 */     return val instanceof Boolean;
/*     */   }
/*     */ 
/*     */   public double getDouble(String path) {
/* 326 */     Double val = (Double)get(path);
/* 327 */     return val!=null?val:0.0D;
/*     */   }
/*     */ 
/*     */   public boolean isDouble(String path) {
/* 331 */     Object val = get(path);
/* 332 */     return val instanceof Double;
/*     */   }
/*     */ 
/*     */   public long getLong(String pat) {
/* 341 */     Long val = (Long)get(path);
/* 342 */     return val!=null?val:0L;
/*     */   }
/*     */ 
/*     */   public boolean isLong(String path) {
/* 346 */     Object val = get(path);
/* 347 */     return val instanceof Long;
/*     */   }
/*     */ 
/*     */   public List<?> getList(String path)
/*     */   {
/* 352 */     Object def = getDefault(path);
/* 353 */     return getList(path, (def instanceof List) ? (List)def : null);
/*     */   }
/*     */ 
/*     */   public List<?> getList(String path, List<?> def) {
/* 357 */     Object val = get(path, def);
/* 358 */     return (List)((val instanceof List) ? val : def);
/*     */   }
/*     */ 
/*     */   public boolean isList(String path) {
/* 362 */     Object val = get(path);
/* 363 */     return val instanceof List;
/*     */   }
/*     */ 
/*     */   public List<String> getStringList(String path) {
/* 367 */     List list = getList(path);
/*     */ 
/* 369 */     if (list == null) {
/* 370 */       return new ArrayList(0);
/*     */     }
/*     */ 
/* 373 */     List result = new ArrayList();
/*     */ 
/* 375 */     for (Iterator i$ = list.iterator(); i$.hasNext(); ) { Object object = i$.next();
/* 376 */       if (((object instanceof String)) || (isPrimitiveWrapper(object))) {
/* 377 */         result.add(String.valueOf(object));
/*     */       }
/*     */     }
/*     */ 
/* 381 */     return result;
/*     */   }
/*     */ 
/*     */   public List<Integer> getIntegerList(String path) {
/* 385 */     List list = getList(path);
/*     */ 
/* 387 */     if (list == null) {
/* 388 */       return new ArrayList(0);
/*     */     }
/*     */ 
/* 391 */     List result = new ArrayList();
/*     */ 
/* 393 */     for (Iterator i$ = list.iterator(); i$.hasNext(); ) { Object object = i$.next();
/* 394 */       if ((object instanceof Integer))
/* 395 */         result.add((Integer)object);
/* 396 */       else if ((object instanceof String))
/*     */         try {
/* 398 */           result.add(Integer.valueOf((String)object));
/*     */         } catch (Exception ex) {
/*     */         }
/* 401 */       else if ((object instanceof Character))
/* 402 */         result.add(Integer.valueOf(((Character)object).charValue()));
/* 403 */       else if ((object instanceof Number)) {
/* 404 */         result.add(Integer.valueOf(((Number)object).intValue()));
/*     */       }
/*     */     }
/*     */ 
/* 408 */     return result;
/*     */   }
/*     */ 
/*     */   public List<Boolean> getBooleanList(String path) {
/* 412 */     List list = getList(path);
/*     */ 
/* 414 */     if (list == null) {
/* 415 */       return new ArrayList(0);
/*     */     }
/*     */ 
/* 418 */     List result = new ArrayList();
/*     */ 
/* 420 */     for (Iterator i$ = list.iterator(); i$.hasNext(); ) { Object object = i$.next();
/* 421 */       if ((object instanceof Boolean))
/* 422 */         result.add((Boolean)object);
/* 423 */       else if ((object instanceof String)) {
/* 424 */         if (Boolean.TRUE.toString().equals(object))
/* 425 */           result.add(Boolean.valueOf(true));
/* 426 */         else if (Boolean.FALSE.toString().equals(object)) {
/* 427 */           result.add(Boolean.valueOf(false));
/*     */         }
/*     */       }
/*     */     }
/*     */ 
/* 432 */     return result;
/*     */   }
/*     */ 
/*     */   public List<Double> getDoubleList(String path) {
/* 436 */     List list = getList(path);
/*     */ 
/* 438 */     if (list == null) {
/* 439 */       return new ArrayList(0);
/*     */     }
/*     */ 
/* 442 */     List result = new ArrayList();
/*     */ 
/* 444 */     for (Iterator i$ = list.iterator(); i$.hasNext(); ) { Object object = i$.next();
/* 445 */       if ((object instanceof Double))
/* 446 */         result.add((Double)object);
/* 447 */       else if ((object instanceof String))
/*     */         try {
/* 449 */           result.add(Double.valueOf((String)object));
/*     */         } catch (Exception ex) {
/*     */         }
/* 452 */       else if ((object instanceof Character))
/* 453 */         result.add(Double.valueOf(((Character)object).charValue()));
/* 454 */       else if ((object instanceof Number)) {
/* 455 */         result.add(Double.valueOf(((Number)object).doubleValue()));
/*     */       }
/*     */     }
/*     */ 
/* 459 */     return result;
/*     */   }
/*     */ 
/*     */   public List<Float> getFloatList(String path) {
/* 463 */     List list = getList(path);
/*     */ 
/* 465 */     if (list == null) {
/* 466 */       return new ArrayList(0);
/*     */     }
/*     */ 
/* 469 */     List result = new ArrayList();
/*     */ 
/* 471 */     for (Iterator i$ = list.iterator(); i$.hasNext(); ) { Object object = i$.next();
/* 472 */       if ((object instanceof Float))
/* 473 */         result.add((Float)object);
/* 474 */       else if ((object instanceof String))
/*     */         try {
/* 476 */           result.add(Float.valueOf((String)object));
/*     */         } catch (Exception ex) {
/*     */         }
/* 479 */       else if ((object instanceof Character))
/* 480 */         result.add(Float.valueOf(((Character)object).charValue()));
/* 481 */       else if ((object instanceof Number)) {
/* 482 */         result.add(Float.valueOf(((Number)object).floatValue()));
/*     */       }
/*     */     }
/*     */ 
/* 486 */     return result;
/*     */   }
/*     */ 
/*     */   public List<Long> getLongList(String path) {
/* 490 */     List list = getList(path);
/*     */ 
/* 492 */     if (list == null) {
/* 493 */       return new ArrayList(0);
/*     */     }
/*     */ 
/* 496 */     List result = new ArrayList();
/*     */ 
/* 498 */     for (Iterator i$ = list.iterator(); i$.hasNext(); ) { Object object = i$.next();
/* 499 */       if ((object instanceof Long))
/* 500 */         result.add((Long)object);
/* 501 */       else if ((object instanceof String))
/*     */         try {
/* 503 */           result.add(Long.valueOf((String)object));
/*     */         } catch (Exception ex) {
/*     */         }
/* 506 */       else if ((object instanceof Character))
/* 507 */         result.add(Long.valueOf(((Character)object).charValue()));
/* 508 */       else if ((object instanceof Number)) {
/* 509 */         result.add(Long.valueOf(((Number)object).longValue()));
/*     */       }
/*     */     }
/*     */ 
/* 513 */     return result;
/*     */   }
/*     */ 
/*     */   public List<Byte> getByteList(String path) {
/* 517 */     List list = getList(path);
/*     */ 
/* 519 */     if (list == null) {
/* 520 */       return new ArrayList(0);
/*     */     }
/*     */ 
/* 523 */     List result = new ArrayList();
/*     */ 
/* 525 */     for (Iterator i$ = list.iterator(); i$.hasNext(); ) { Object object = i$.next();
/* 526 */       if ((object instanceof Byte))
/* 527 */         result.add((Byte)object);
/* 528 */       else if ((object instanceof String))
/*     */         try {
/* 530 */           result.add(Byte.valueOf((String)object));
/*     */         } catch (Exception ex) {
/*     */         }
/* 533 */       else if ((object instanceof Character))
/* 534 */         result.add(Byte.valueOf((byte)((Character)object).charValue()));
/* 535 */       else if ((object instanceof Number)) {
/* 536 */         result.add(Byte.valueOf(((Number)object).byteValue()));
/*     */       }
/*     */     }
/*     */ 
/* 540 */     return result;
/*     */   }
/*     */ 
/*     */   public List<Character> getCharacterList(String path) {
/* 544 */     List list = getList(path);
/*     */ 
/* 546 */     if (list == null) {
/* 547 */       return new ArrayList(0);
/*     */     }
/*     */ 
/* 550 */     List result = new ArrayList();
/*     */ 
/* 552 */     for (Iterator i$ = list.iterator(); i$.hasNext(); ) { Object object = i$.next();
/* 553 */       if ((object instanceof Character)) {
/* 554 */         result.add((Character)object);
/* 555 */       } else if ((object instanceof String)) {
/* 556 */         String str = (String)object;
/*     */ 
/* 558 */         if (str.length() == 1)
/* 559 */           result.add(Character.valueOf(str.charAt(0)));
/*     */       }
/* 561 */       else if ((object instanceof Number)) {
/* 562 */         result.add(Character.valueOf((char)((Number)object).intValue()));
/*     */       }
/*     */     }
/*     */ 
/* 566 */     return result;
/*     */   }
/*     */ 
/*     */   public List<Short> getShortList(String path) {
/* 570 */     List list = getList(path);
/*     */ 
/* 572 */     if (list == null) {
/* 573 */       return new ArrayList(0);
/*     */     }
/*     */ 
/* 576 */     List result = new ArrayList();
/*     */ 
/* 578 */     for (Iterator i$ = list.iterator(); i$.hasNext(); ) { Object object = i$.next();
/* 579 */       if ((object instanceof Short))
/* 580 */         result.add((Short)object);
/* 581 */       else if ((object instanceof String))
/*     */         try {
/* 583 */           result.add(Short.valueOf((String)object));
/*     */         } catch (Exception ex) {
/*     */         }
/* 586 */       else if ((object instanceof Character))
/* 587 */         result.add(Short.valueOf((short)((Character)object).charValue()));
/* 588 */       else if ((object instanceof Number)) {
/* 589 */         result.add(Short.valueOf(((Number)object).shortValue()));
/*     */       }
/*     */     }
/*     */ 
/* 593 */     return result;
/*     */   }
/*     */ 
/*     */   public List<Map<?, ?>> getMapList(String path) {
/* 597 */     List list = getList(path);
/* 598 */     List result = new ArrayList();
/*     */ 
/* 600 */     if (list == null) {
/* 601 */       return result;
/*     */     }
/*     */ 
/* 604 */     for (Iterator i$ = list.iterator(); i$.hasNext(); ) { Object object = i$.next();
/* 605 */       if ((object instanceof Map)) {
/* 606 */         result.add((Map)object);
/*     */       }
/*     */     }
/*     */ 
/* 610 */     return result;
/*     */   }
/*     */ 
/*     */   public ConfigurationSection getConfigurationSection(String path) {
/* 675 */     Object val = get(path, null);
/* 676 */     if (val != null) {
/* 677 */       return (val instanceof ConfigurationSection) ? (ConfigurationSection)val : null;
/*     */     }
/*     */ 
/* 680 */     val = get(path, getDefault(path));
/* 681 */     return (val instanceof ConfigurationSection) ? createSection(path) : null;
/*     */   }
/*     */ 
/*     */   public boolean isConfigurationSection(String path) {
/* 685 */     Object val = get(path);
/* 686 */     return val instanceof ConfigurationSection;
/*     */   }
/*     */ 
/*     */   protected boolean isPrimitiveWrapper(Object input) {
/* 690 */     return ((input instanceof Integer)) || ((input instanceof Boolean)) || ((input instanceof Character)) || ((input instanceof Byte)) || ((input instanceof Short)) || ((input instanceof Double)) || ((input instanceof Long)) || ((input instanceof Float));
/*     */   }
/*     */ 
/*     */   protected Object getDefault(String path)
/*     */   {
/* 697 */     Validate.notNull(path, "Path cannot be null");
/*     */ 
/* 699 */     Configuration root = getRoot();
/* 700 */     Configuration defaults = root == null ? null : root.getDefaults();
/* 701 */     return defaults == null ? null : defaults.get(createPath(this, path));
/*     */   }
/*     */ 
/*     */   protected void mapChildrenKeys(Set<String> output, ConfigurationSection section, boolean deep) {
/* 705 */     if ((section instanceof MemorySection)) {
/* 706 */       MemorySection sec = (MemorySection)section;
/*     */ 
/* 708 */       for (Map.Entry entry : sec.map.entrySet()) {
/* 709 */         output.add(createPath(section, (String)entry.getKey(), this));
/*     */ 
/* 711 */         if ((deep) && ((entry.getValue() instanceof ConfigurationSection))) {
/* 712 */           ConfigurationSection subsection = (ConfigurationSection)entry.getValue();
/* 713 */           mapChildrenKeys(output, subsection, deep);
/*     */         }
/*     */       }
/*     */     } else {
/* 717 */       Set<String> keys = section.getKeys(deep);
/*     */ 
/* 719 */       for (String key : keys)
/* 720 */         output.add(createPath(section, key, this));
/*     */     }
/*     */   }
/*     */ 
/*     */   protected void mapChildrenValues(Map<String, Object> output, ConfigurationSection section, boolean deep)
/*     */   {
/* 726 */     if ((section instanceof MemorySection)) {
/* 727 */       MemorySection sec = (MemorySection)section;
/*     */ 
/* 729 */       for (Map.Entry entry : sec.map.entrySet()) {
/* 730 */         output.put(createPath(section, (String)entry.getKey(), this), entry.getValue());
/*     */ 
/* 732 */         if (((entry.getValue() instanceof ConfigurationSection)) && 
/* 733 */           (deep))
/* 734 */           mapChildrenValues(output, (ConfigurationSection)entry.getValue(), deep);
/*     */       }
/*     */     }
/*     */     else
/*     */     {
/* 739 */       Map<String, Object> values = section.getValues(deep);
/*     */ 
/* 741 */       for (Map.Entry entry : values.entrySet())
/* 742 */         output.put(createPath(section, (String)entry.getKey(), this), entry.getValue());
/*     */     }
/*     */   }
/*     */ 
/*     */   public static String createPath(ConfigurationSection section, String key)
/*     */   {
/* 757 */     return createPath(section, key, section == null ? null : section.getRoot());
/*     */   }
/*     */ 
/*     */   public static String createPath(ConfigurationSection section, String key, ConfigurationSection relativeTo)
/*     */   {
/* 771 */     Validate.notNull(section, "Cannot create path without a section");
/* 772 */     Configuration root = section.getRoot();
/* 773 */     if (root == null) {
/* 774 */       throw new IllegalStateException("Cannot create path without a root");
/*     */     }
/* 776 */     char separator = root.options().pathSeparator();
/*     */ 
/*     */     StringBuilder builder = new StringBuilder();
/*     */     if (section != null) {
/*     */       for (ConfigurationSection parent = section; (parent != null) && (parent != relativeTo); parent = parent.getParent()) {
/*     */         if (builder.length() > 0) {
/*     */           builder.insert(0, separator);
/*     */         }
/*     */ 
/*     */         builder.insert(0, parent.getName());
/*     */       }
/*     */     }
/*     */ 
/*     */     if ((key != null) && (key.length() > 0)) {
/*     */       if (builder.length() > 0) {
/*     */         builder.append(separator);
/*     */       }
/*     */ 
/*     */       builder.append(key);
/*     */     }
/*     */ 
/*     */     return builder.toString();
/*     */   }
/*     */ 
/*     */   public String toString()
/*     */   {
/*     */     Configuration root = getRoot();
/*     */     return new StringBuilder().append(getClass().getSimpleName()).append("[path='").append(getCurrentPath()).append("', root='").append(root == null ? null : root.getClass().getSimpleName()).append("']").toString();
/*     */   }
/*     */ }