package psp2.UltiNaruto.VitaPadClient.configuration;

import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract interface ConfigurationSection
{
  public abstract Set<String> getKeys(boolean paramBoolean);

  public abstract Map<String, Object> getValues(boolean paramBoolean);

  public abstract boolean contains(String paramString);

  public abstract boolean isSet(String paramString);

  public abstract String getCurrentPath();

  public abstract String getName();

  public abstract Configuration getRoot();

  public abstract ConfigurationSection getParent();

  public abstract Object get(String paramString);

  public abstract Object get(String paramString, Object paramObject);

  public abstract void set(String paramString, Object paramObject);

  public abstract ConfigurationSection createSection(String paramString);

  public abstract ConfigurationSection createSection(String paramString, Map<?, ?> paramMap);

  public abstract String getString(String paramString);

  public abstract String getString(String paramString1, String paramString2);

  public abstract boolean isString(String paramString);

  public abstract int getInt(String paramString);

  public abstract boolean isInt(String paramString);

  public abstract boolean getBoolean(String paramString);

  public abstract boolean getBoolean(String paramString, boolean paramBoolean);

  public abstract boolean isBoolean(String paramString);

  public abstract double getDouble(String paramString);

  public abstract boolean isDouble(String paramString);

  public abstract long getLong(String paramString);

  public abstract boolean isLong(String paramString);

  public abstract List<?> getList(String paramString);

  public abstract List<?> getList(String paramString, List<?> paramList);

  public abstract boolean isList(String paramString);

  public abstract List<String> getStringList(String paramString);

  public abstract List<Integer> getIntegerList(String paramString);

  public abstract List<Boolean> getBooleanList(String paramString);

  public abstract List<Double> getDoubleList(String paramString);

  public abstract List<Float> getFloatList(String paramString);

  public abstract List<Long> getLongList(String paramString);

  public abstract List<Byte> getByteList(String paramString);

  public abstract List<Character> getCharacterList(String paramString);

  public abstract List<Short> getShortList(String paramString);

  public abstract List<Map<?, ?>> getMapList(String paramString);

  public abstract ConfigurationSection getConfigurationSection(String paramString);

  public abstract boolean isConfigurationSection(String paramString);

  public abstract ConfigurationSection getDefaultSection();

  public abstract void addDefault(String paramString, Object paramObject);
}
