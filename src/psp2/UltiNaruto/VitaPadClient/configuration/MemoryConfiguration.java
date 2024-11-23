package psp2.UltiNaruto.VitaPadClient.configuration;

import java.util.Map;

import org.apache.commons.lang3.Validate;

public class MemoryConfiguration extends MemorySection
implements Configuration
{
	protected Configuration defaults;
	protected MemoryConfigurationOptions options;

	public MemoryConfiguration()
	{
	}

	public MemoryConfiguration(Configuration defaults)
	{
		this.defaults = defaults;
	}

	public void addDefault(String path, Object value)
	{
		Validate.notNull(path, "Path may not be null");

		if (this.defaults == null) {
			this.defaults = new MemoryConfiguration();
		}

		this.defaults.set(path, value);
	}

	public void addDefaults(Map<String, Object> defaults) {
		Validate.notNull(defaults, "Defaults may not be null");

		for (Map.Entry<String, Object> entry : defaults.entrySet())
			addDefault((String)entry.getKey(), entry.getValue());
	}

	public void addDefaults(Configuration defaults)
	{
		Validate.notNull(defaults, "Defaults may not be null");

		addDefaults(defaults.getValues(true));
	}

	public void setDefaults(Configuration defaults) {
		Validate.notNull(defaults, "Defaults may not be null");

		this.defaults = defaults;
	}

	public Configuration getDefaults() {
		return this.defaults;
	}

	public ConfigurationSection getParent()
	{
		return null;
	}

	public MemoryConfigurationOptions options() {
		if (this.options == null) {
			this.options = new MemoryConfigurationOptions(this);
		}

		return this.options;
	}
}
