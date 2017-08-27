package psp2.UltiNaruto.VitaPadClient.configuration.file;

import java.util.LinkedHashMap;
import java.util.Map;

import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.representer.Representer;
import org.yaml.snakeyaml.representer.SafeRepresenter;

import psp2.UltiNaruto.VitaPadClient.configuration.ConfigurationSection;
import psp2.UltiNaruto.VitaPadClient.configuration.serialization.ConfigurationSerializable;
import psp2.UltiNaruto.VitaPadClient.configuration.serialization.ConfigurationSerialization;

public class YamlRepresenter extends Representer
{
	public YamlRepresenter()
	{
		this.multiRepresenters.put(ConfigurationSection.class, new RepresentConfigurationSection());
		this.multiRepresenters.put(ConfigurationSerializable.class, new RepresentConfigurationSerializable());
	}

	private class RepresentConfigurationSerializable extends SafeRepresenter.RepresentMap
	{
		private RepresentConfigurationSerializable()
		{
			super();
		}
		public Node representData(Object data) {
			ConfigurationSerializable serializable = (ConfigurationSerializable)data;
			Map values = new LinkedHashMap();
			values.put("==", ConfigurationSerialization.getAlias(serializable.getClass()));
			values.putAll(serializable.serialize());

			return super.representData(values);
		}
	}

	private class RepresentConfigurationSection extends SafeRepresenter.RepresentMap
	{
		private RepresentConfigurationSection()
		{
			super();
		}
		public Node representData(Object data) {
			return super.representData(((ConfigurationSection)data).getValues(false));
		}
	}
}