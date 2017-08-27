package psp2.UltiNaruto.VitaPadClient.configuration.file; 

import psp2.UltiNaruto.VitaPadClient.configuration.Configuration;
import psp2.UltiNaruto.VitaPadClient.configuration.InvalidConfigurationException;
import psp2.UltiNaruto.VitaPadClient.configuration.MemoryConfiguration;
import com.google.common.io.Files;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import org.apache.commons.lang3.Validate;

public abstract class FileConfiguration extends MemoryConfiguration
{
	public FileConfiguration()
	{
	}

	public FileConfiguration(Configuration defaults)
	{
		super(defaults);
	}

	public void save(File file) throws IOException
	{
		Validate.notNull(file, "File cannot be null");

		Files.createParentDirs(file);

		String data = saveToString();

		FileWriter writer = new FileWriter(file);
		try
		{
			writer.write(data);
		} finally {
			writer.close();
		}
	}

	public void save(String file) throws IOException
	{
		Validate.notNull(file, "File cannot be null");

		save(new File(file));
	}

	public abstract String saveToString();

	public void load(File file)	throws FileNotFoundException, IOException, InvalidConfigurationException
	{
		Validate.notNull(file, "File cannot be null");

		load(new FileInputStream(file));
	}

	public void load(InputStream stream) throws IOException, InvalidConfigurationException
	{
		Validate.notNull(stream, "Stream cannot be null");

		InputStreamReader reader = new InputStreamReader(stream);
		StringBuilder builder = new StringBuilder();
		BufferedReader input = new BufferedReader(reader);
		try
		{
			String line;
			while ((line = input.readLine()) != null) {
				builder.append(line);
				builder.append('\n');
			}
		} finally {
			input.close();
		}

		loadFromString(builder.toString());
	}

	public void load(String file) throws FileNotFoundException, IOException, InvalidConfigurationException
	{
		Validate.notNull(file, "File cannot be null");

		load(new File(file));
	}

	public abstract void loadFromString(String paramString)
			throws InvalidConfigurationException;

	protected abstract String buildHeader();

	public FileConfigurationOptions options()
	{
		if (this.options == null) {
			this.options = new FileConfigurationOptions(this);
		}

		return (FileConfigurationOptions)this.options;
	}
}