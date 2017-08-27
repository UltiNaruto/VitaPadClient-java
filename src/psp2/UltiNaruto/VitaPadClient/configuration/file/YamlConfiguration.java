package psp2.UltiNaruto.VitaPadClient.configuration.file;
/*     */ 
/*     */ import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.apache.commons.lang3.Validate;
import org.apache.logging.log4j.LogManager;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;
import org.yaml.snakeyaml.representer.Representer;

import psp2.UltiNaruto.VitaPadClient.configuration.Configuration;
import psp2.UltiNaruto.VitaPadClient.configuration.ConfigurationSection;
import psp2.UltiNaruto.VitaPadClient.configuration.InvalidConfigurationException;
/*     */ 
/*     */ public class YamlConfiguration extends FileConfiguration
/*     */ {
/*     */   protected static final String COMMENT_PREFIX = "# ";
/*     */   protected static final String BLANK_CONFIG = "{}\n";
/*  27 */   private final DumperOptions yamlOptions = new DumperOptions();
/*  28 */   private final Representer yamlRepresenter = new YamlRepresenter();
/*  29 */   private final Yaml yaml = new Yaml(new YamlConstructor(), this.yamlRepresenter, this.yamlOptions);
/*     */ 
/*     */   public String saveToString()
/*     */   {
/*  33 */     this.yamlOptions.setIndent(options().indent());
/*  34 */     this.yamlOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
/*  35 */     this.yamlRepresenter.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
/*     */ 
/*  37 */     String header = buildHeader();
/*  38 */     String dump = this.yaml.dump(getValues(false));
/*     */ 
/*  40 */     if (dump.equals("{}\n")) {
/*  41 */       dump = "";
/*     */     }
/*     */ 
/*  44 */     return new StringBuilder().append(header).append(dump).toString();
/*     */   }
/*     */ 
/*     */   public void loadFromString(String contents) throws InvalidConfigurationException {
/*  49 */     Validate.notNull(contents, "Contents cannot be null");
/*     */     Map input;
/*     */     try {
/*  53 */       input = (Map)this.yaml.load(contents);
/*     */     } catch (YAMLException e) {
/*  55 */       throw new InvalidConfigurationException(e);
/*     */     } catch (ClassCastException e) {
/*  57 */       throw new InvalidConfigurationException("Top level is not a Map.");
/*     */     }
/*     */ 
/*  60 */     String header = parseHeader(contents);
/*  61 */     if (header.length() > 0) {
/*  62 */       options().header(header);
/*     */     }
/*     */ 
/*  65 */     if (input != null)
/*  66 */       convertMapsToSections(input, this);
/*     */   }
/*     */ 
/*     */   protected void convertMapsToSections(Map<?, ?> input, ConfigurationSection section)
/*     */   {
/*  71 */     for (Map.Entry entry : input.entrySet()) {
/*  72 */       String key = entry.getKey().toString();
/*  73 */       Object value = entry.getValue();
/*     */ 
/*  75 */       if ((value instanceof Map))
/*  76 */         convertMapsToSections((Map)value, section.createSection(key));
/*     */       else
/*  78 */         section.set(key, value);
/*     */     }
/*     */   }
/*     */ 
/*     */   protected String parseHeader(String input)
/*     */   {
/*  84 */     String[] lines = input.split("\r?\n", -1);
/*  85 */     StringBuilder result = new StringBuilder();
/*  86 */     boolean readingHeader = true;
/*  87 */     boolean foundHeader = false;
/*     */ 
/*  89 */     for (int i = 0; (i < lines.length) && (readingHeader); i++) {
/*  90 */       String line = lines[i];
/*     */ 
/*  92 */       if (line.startsWith("# ")) {
/*  93 */         if (i > 0) {
/*  94 */           result.append("\n");
/*     */         }
/*     */ 
/*  97 */         if (line.length() > "# ".length()) {
/*  98 */           result.append(line.substring("# ".length()));
/*     */         }
/*     */ 
/* 101 */         foundHeader = true;
/* 102 */       } else if ((foundHeader) && (line.length() == 0)) {
/* 103 */         result.append("\n");
/* 104 */       } else if (foundHeader) {
/* 105 */         readingHeader = false;
/*     */       }
/*     */     }
/*     */ 
/* 109 */     return result.toString();
/*     */   }
/*     */ 
/*     */   protected String buildHeader()
/*     */   {
/* 114 */     String header = options().header();
/*     */ 
/* 116 */     if (options().copyHeader()) {
/* 117 */       Configuration def = getDefaults();
/*     */ 
/* 119 */       if ((def != null) && ((def instanceof FileConfiguration))) {
/* 120 */         FileConfiguration filedefaults = (FileConfiguration)def;
/* 121 */         String defaultsHeader = filedefaults.buildHeader();
/*     */ 
/* 123 */         if ((defaultsHeader != null) && (defaultsHeader.length() > 0)) {
/* 124 */           return defaultsHeader;
/*     */         }
/*     */       }
/*     */     }
/*     */ 
/* 129 */     if (header == null) {
/* 130 */       return "";
/*     */     }
/*     */ 
/* 133 */     StringBuilder builder = new StringBuilder();
/* 134 */     String[] lines = header.split("\r?\n", -1);
/* 135 */     boolean startedHeader = false;
/*     */ 
/* 137 */     for (int i = lines.length - 1; i >= 0; i--) {
/* 138 */       builder.insert(0, "\n");
/*     */ 
/* 140 */       if ((startedHeader) || (lines[i].length() != 0)) {
/* 141 */         builder.insert(0, lines[i]);
/* 142 */         builder.insert(0, "# ");
/* 143 */         startedHeader = true;
/*     */       }
/*     */     }
/*     */ 
/* 147 */     return builder.toString();
/*     */   }
/*     */ 
/*     */   public YamlConfigurationOptions options()
/*     */   {
/* 152 */     if (this.options == null) {
/* 153 */       this.options = new YamlConfigurationOptions(this);
/*     */     }
/*     */ 
/* 156 */     return (YamlConfigurationOptions)this.options;
/*     */   }
/*     */ 
/*     */   public static YamlConfiguration loadConfiguration(File file)
/*     */   {
/* 170 */     Validate.notNull(file, "File cannot be null");
/*     */ 
/* 172 */     YamlConfiguration config = new YamlConfiguration();
/*     */     try
/*     */     {
/* 175 */       config.load(file);
/*     */     } catch (FileNotFoundException ex) {
/*     */     } catch (IOException ex) {
/* 178 */       LogManager.getLogger().error(new StringBuilder().append("[SEVERE] Cannot load ").append(file).toString(), ex);
/*     */     } catch (InvalidConfigurationException ex) {
/* 180 */       LogManager.getLogger().error(new StringBuilder().append("[SEVERE] Cannot load ").append(file).toString(), ex);
/*     */     }
/*     */ 
/* 183 */     return config;
/*     */   }
/*     */ 
/*     */   public static YamlConfiguration loadConfiguration(InputStream stream)
/*     */   {
/* 197 */     Validate.notNull(stream, "Stream cannot be null");
/*     */ 
/* 199 */     YamlConfiguration config = new YamlConfiguration();
/*     */     try
/*     */     {
/* 202 */       config.load(stream);
/*     */     } catch (IOException ex) {
/* 204 */       LogManager.getLogger().error("[SEVERE] Cannot load configuration from stream", ex);
/*     */     } catch (InvalidConfigurationException ex) {
/* 206 */       LogManager.getLogger().error("[SEVERE] Cannot load configuration from stream", ex);
/*     */     }
/*     */ 
/* 209 */     return config;
/*     */   }
/*     */ }
