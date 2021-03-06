/*
 * Copyright 2012 Decebal Suiu
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this work except in compliance with
 * the License. You may obtain a copy of the License in the LICENSE file, or at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ro.fortsoft.pf4j;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.SecretKey;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * A plugin descriptor contains information about a plug-in obtained
 * from the manifest (META-INF) file.
 *
 * @author Decebal Suiu, Leander Kurscheidt, Julian Brendl
 */
public class PluginDescriptor {
    private final static String SDK_PLUGIN_ID = "org.intellimate.izou.sdk";
	private String pluginId;
	private String pluginDescription;
    private String pluginClass;
    private Version version;
    private String rawVersion;
    private SecretKey secureID;
    private boolean secureIDSet = false;
    private Version requires;
    private String provider;
    private String title;
    private String artifactID;
    private int serverID;
    private Version sdkVersion;
    private List<PluginDependency> dependencies;
    private PluginManager pluginManager;
    private Properties addOnProperties;
    private static final Logger log = LoggerFactory.getLogger(IzouPluginClassLoader.class);

    public PluginDescriptor(PluginManager pluginManager) {
    	requires = Version.ZERO;
        dependencies = new ArrayList<>();
        this.pluginManager = pluginManager;
    }

    /**
     * Returns the unique identifier of this plugin.
     */
    public String getPluginId() {
        return pluginId;
    }

    /**
     * Returns the description of this plugin.
     */
    public String getPluginDescription() {
        return pluginDescription;
    }

    /**
     * Returns the name of the class that implements Plugin interface.
     */
    public String getPluginClass() {
        return pluginClass;
    }

    /**
     * Returns the version of this plugin.
     */
    public Version getVersion() {
        return version;
    }

    /**
     * Returns the requires of this plugin.
     */
    public Version getRequires() {
        return requires;
    }

    /**
     * Returns the provider name of this plugin.
     */
    public String getProvider() {
        return provider;
    }

    /**
     * Returns the implementation title from the manifest of this plugin.
     */
    public String getTitle() {
        return title;
    }

    public Optional<Integer> getServerID() {
        if (serverID != -1) {
            return Optional.of(serverID);
        } else {
            return Optional.empty();
        }
    }

    public Version getSdkVersion() {
        return sdkVersion;
    }

    public String getArtifactID() {
        return artifactID;
    }

    /**
     * Returns all dependencies declared by this plugin.
     * Returns an empty array if this plugin does not declare any require.
     */
    public List<PluginDependency> getDependencies() {
        return dependencies.stream()
                .filter(dependency -> pluginManager.getPlugin(dependency.getPluginId()) != null)
                .collect(Collectors.toList());
    }

    @Override
	public String toString() {
		return "PluginDescriptor [pluginId=" + pluginId + ", pluginClass="
				+ pluginClass + ", version=" + version + ", provider="
				+ provider + ", dependencies=" + dependencies
				+ "]";
	}

	void setPluginId(String pluginId) {
        this.pluginId = pluginId;
    }

	void setPluginDescription(String pluginDescription) {
        this.pluginDescription = pluginDescription;
    }

    void setPluginClass(String pluginClassName) {
        this.pluginClass = pluginClassName;
    }

    void setPluginVersion(Version version, String raw) {
        this.version = version;
        this.rawVersion = raw;
    }

    void setProvider(String provider) {
        this.provider = provider;
    }

    void setTitle(String title) {
        this.title = title;
    }

    void setServerID(String serverID) {
        if (serverID != null && !serverID.equals("")) {
            try {
                this.serverID = Integer.parseInt(serverID);
            } catch (NumberFormatException e) {
                log.error("Unable to parse server ID for addOn: " + title, e);
            }
        } else {
            this.serverID = -1;
        }
    }

    void setSDKVersion(String sdkVersion) {
        this.sdkVersion = Version.createVersion(sdkVersion);
    }

    void setArtifactID(String artifactID) {
        this.artifactID = artifactID;
    }

    void setRequires(Version requires) {
        this.requires = requires;
    }

    void setDependencies(String dependencies) {
    	if (dependencies != null) {
    		dependencies = dependencies.trim();
    		if (dependencies.isEmpty()) {
    			this.dependencies = new ArrayList<>();
    		} else {
	    		this.dependencies = new ArrayList<>();
	    		String[] tokens = dependencies.split(",");
	    		for (String dependency : tokens) {
	    			dependency = dependency.trim();
	    			if (!dependency.isEmpty()) {
	    				this.dependencies.add(new PluginDependency(dependency));
	    			}
	    		}
	    		if (this.dependencies.isEmpty()) {
	    			this.dependencies = Collections.emptyList();
	    		}
    		}
    	} else {
    		this.dependencies = new ArrayList<>();
    	}
        addSDKDependency();
    }

    void setAddOnProperties() {
        String[] split = pluginId.split("\\.");
        File descriptorFile = new File(pluginManager.getPluginDirectory().getPath() + File.separator +
                split[split.length - 1] + "-" + rawVersion + File.separator + "classes" + File.separator
                + "addon_config.properties");

        if (descriptorFile.exists()) {
            try(FileInputStream fileInput = new FileInputStream(descriptorFile)) {
                Properties properties = new Properties();
                properties.load(fileInput);
                fileInput.close();
                addOnProperties = properties;
            } catch (IOException e) {
                log.error("Error while trying to read class_loader_config.properties file for addOn:" + pluginId
                        + ", is it a resource in your addOn? Are all required properties there and filled out?", e);
            }
        } else {
            if (!pluginId.equals(SDK_PLUGIN_ID)) {
                log.error("Error, no config file found for plugin: " + pluginId);
            }
        }
    }

    private void addSDKDependency() {
        if (pluginId.equals(SDK_PLUGIN_ID)) {
            return;
        }

        Version sdkVersion = getSdkVersion();
        if (sdkVersion == null) {
            log.error("Error, sdk-version not found in manifest for " + pluginId);
            addDefaultSDKDependency();
            return;
        }

        PluginDependency pluginDependency = new PluginDependency(SDK_PLUGIN_ID);
        pluginDependency.setPluginVersion(new Version(sdkVersion.getMajor(), 0, 0));
        dependencies.add(pluginDependency);
    }

    /**
     * Sets the secure (unique) ID of this plugin
     * <p>
     *     This method can only be called once per object
     * </p>
     */
    public void setSecureID(SecretKey id) {
        if (!secureIDSet) {
            secureID = id;
            secureIDSet = true;
        }
    }

    /**
     * Gets the secure (unique) ID of this plugin
     *
     * @return ts the secure (unique) ID of this plugin
     */
    public SecretKey getSecureID() {
        return secureID;
    }

    /**
     * Adds the sdk as a dependency
     */
    public void addDefaultSDKDependency() {
        dependencies.add(new PluginDependency(SDK_PLUGIN_ID));
    }

    /**
     * Gets the properties object associated with this plugin (addOn)
     * @return the properties object associated with this plugin (addOn) 
     */
    public Properties getAddOnProperties() {
        return addOnProperties;
    }
}
