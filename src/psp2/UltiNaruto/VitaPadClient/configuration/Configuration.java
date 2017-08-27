package psp2.UltiNaruto.VitaPadClient.configuration;

import java.util.Map;

public abstract interface Configuration extends ConfigurationSection
{
  public abstract void addDefault(String paramString, Object paramObject);

  public abstract void addDefaults(Map<String, Object> paramMap);

  public abstract void addDefaults(Configuration paramConfiguration);

  public abstract void setDefaults(Configuration paramConfiguration);

  public abstract Configuration getDefaults();

  public abstract ConfigurationOptions options();
}