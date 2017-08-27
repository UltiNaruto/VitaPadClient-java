package psp2.UltiNaruto.VitaPadClient.configuration.serialization;

import java.util.Map;

public abstract interface ConfigurationSerializable
{
  public abstract Map<String, Object> serialize();
}