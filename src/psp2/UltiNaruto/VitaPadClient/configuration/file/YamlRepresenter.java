package psp2.UltiNaruto.VitaPadClient.configuration.file;

import java.util.LinkedHashMap;
import java.util.Map;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Represent;
import org.yaml.snakeyaml.representer.Representer;

import psp2.UltiNaruto.VitaPadClient.configuration.ConfigurationSection;
import psp2.UltiNaruto.VitaPadClient.configuration.serialization.ConfigurationSerializable;
import psp2.UltiNaruto.VitaPadClient.configuration.serialization.ConfigurationSerialization;

public class YamlRepresenter extends Representer
{
	public YamlRepresenter(DumperOptions dumperOptions)
	{
		super(dumperOptions);
		this.multiRepresenters.put(ConfigurationSection.class, new RepresentConfigurationSection());
		this.multiRepresenters.put(ConfigurationSerializable.class, new RepresentConfigurationSerializable());
	}

	private class RepresentConfigurationSerializable implements Represent
	{
		private RepresentConfigurationSerializable()
		{
			super();
		}
		public Node representData(Object data) {
			ConfigurationSerializable serializable = (ConfigurationSerializable)data;
			Map<String, Object> values = new LinkedHashMap<String, Object>();
			values.put("==", ConfigurationSerialization.getAlias(serializable.getClass()));
			values.putAll(serializable.serialize());

			return representMapping(getTag(data.getClass(), Tag.MAP), values, null);
		}
	}

	private class RepresentConfigurationSection implements Represent
	{
		private RepresentConfigurationSection()
		{
			super();
		}
		public Node representData(Object data) {
			Map<String, Object> values = ((ConfigurationSection)data).getValues(false);

			return representMapping(getTag(data.getClass(), Tag.MAP), values, DumperOptions.FlowStyle.BLOCK);
		}
	}
}