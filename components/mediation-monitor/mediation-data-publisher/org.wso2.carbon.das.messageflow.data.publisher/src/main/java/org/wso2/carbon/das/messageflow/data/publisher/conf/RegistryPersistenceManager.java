/*
 * Copyright 2004,2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wso2.carbon.das.messageflow.data.publisher.conf;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.das.data.publisher.util.DASDataPublisherConstants;
import org.wso2.carbon.das.messageflow.data.publisher.util.MediationDataPublisherConstants;
import org.wso2.carbon.das.messageflow.data.publisher.util.MediationPublisherException;
import org.wso2.carbon.registry.core.Registry;
import org.wso2.carbon.registry.core.Resource;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.core.service.RegistryService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class RegistryPersistenceManager {

    private static Log log = LogFactory.getLog(RegistryPersistenceManager.class);
    private static RegistryService dasRegistryService;
    public static final String EMPTY_STRING = "";
    public static final String DAS_SERVER_ID = "DAS_server_id";
    private static String IDS = "ids";

    public static void setDasRegistryService(RegistryService registryServiceParam) {
        dasRegistryService = registryServiceParam;
    }

    public MediationStatConfig get(String serverId, int tenantId) {
        MediationStatConfig mediationStatConfig = new MediationStatConfig();

        // First set it to defaults, but do not persist
        mediationStatConfig.setMessageFlowTracePublishingEnabled(false);
        mediationStatConfig.setMessageFlowStatsPublishingEnabled(false);
        mediationStatConfig.setUrl(EMPTY_STRING);
        mediationStatConfig.setUserName(EMPTY_STRING);
        mediationStatConfig.setPassword(EMPTY_STRING);

        try {
            Registry registry = dasRegistryService.getConfigSystemRegistry(tenantId);
            String resourcePath = MediationDataPublisherConstants.DAS_MEDIATION_MESSAGE_FLOW_REG_PATH + serverId;
            Properties configs = null;

            if (registry != null && registry.resourceExists(resourcePath)) {
                Resource resource = registry.get(resourcePath);
                configs = resource.getProperties();
            } else {
                log.error("Resource not found from registry: " + resourcePath);
                return null;
            }

            if (configs != null) {
                String serverIdRecorded = ((List<String>) configs.get(DAS_SERVER_ID)).get(0);//((List<String>) configs.get(DAS_SERVER_ID)).get(0);
                String url = ((List<String>) configs.get(DASDataPublisherConstants.DAS_URL)).get(0);//(String)configs.get(DASDataPublisherConstants.DAS_URL);
                String userName = ((List<String>) configs.get(DASDataPublisherConstants.DAS_USER_NAME)).get(0);//(String)configs.get(DASDataPublisherConstants.DAS_USER_NAME);
                String password = ((List<String>) configs.get(DASDataPublisherConstants.DAS_PASSWORD)).get(0);//(String)configs.get(DASDataPublisherConstants.DAS_PASSWORD);
                String tracePublishingEnable = ((List<String>) configs.get(DASDataPublisherConstants.DAS_TRACE_PUBLISHING_ENABLED)).get(0);//(String)configs.get(DASDataPublisherConstants.DAS_TRACE_PUBLISHING_ENABLED);
                String statsPublishingEnable = ((List<String>) configs.get(DASDataPublisherConstants.DAS_STATS_PUBLISHING_ENABLED)).get(0);//(String)configs.get(DASDataPublisherConstants.DAS_STATS_PUBLISHING_ENABLED);

                if (url != null && userName != null && password != null) {
                    mediationStatConfig.setMessageFlowTracePublishingEnabled(Boolean.parseBoolean(tracePublishingEnable));
                    mediationStatConfig.setMessageFlowStatsPublishingEnabled(Boolean.parseBoolean(statsPublishingEnable));
                    mediationStatConfig.setServerId(serverIdRecorded);
                    mediationStatConfig.setUrl(url);
                    mediationStatConfig.setUserName(userName);
                    mediationStatConfig.setPassword(password);
                }
            }
        } catch (Exception e) {
            log.error("Could not load values from registry", e);
        }

        return mediationStatConfig;
    }

    public void update(MediationStatConfig config, int tenantId) {
        try {
            Registry registry = dasRegistryService.getConfigSystemRegistry(tenantId);
            String serverId = config.getServerId();
            String resourcePath = MediationDataPublisherConstants.DAS_MEDIATION_MESSAGE_FLOW_REG_PATH + serverId;
            Resource resource;

            if (registry != null) {
                if (registry.resourceExists(resourcePath)) {
                    resource = registry.get(resourcePath);
                } else {
                    resource = registry.newResource();
                }

                resource.addProperty(DAS_SERVER_ID, config.getServerId());
                resource.addProperty(DASDataPublisherConstants.DAS_URL, config.getUrl());
                resource.addProperty(DASDataPublisherConstants.DAS_USER_NAME, config.getUserName());
                resource.addProperty(DASDataPublisherConstants.DAS_PASSWORD, config.getPassword());
                resource.addProperty(DASDataPublisherConstants.DAS_TRACE_PUBLISHING_ENABLED, String.valueOf(config.isMessageFlowTracePublishingEnabled()));
                resource.addProperty(DASDataPublisherConstants.DAS_STATS_PUBLISHING_ENABLED, String.valueOf(config.isMessageFlowStatsPublishingEnabled()));

                // update registry at the end
                registry.put(resourcePath, resource);

                // update the list of server-IDs
                String serverListPath = MediationDataPublisherConstants.DAS_SERVER_LIST_REG_PATH;
                if (registry.resourceExists(serverListPath)) {
                    Resource listResource = registry.get(serverListPath);
                    List<String> idList = listResource.getPropertyValues(IDS);
                    if (idList == null) {
                        idList = new ArrayList<>();
                    }
                    idList.add(serverId);
                    listResource.setProperty(IDS, idList);
                    registry.put(serverListPath, listResource);
                }
            } else {
                log.error("Resource not found from registry: " + resourcePath);
                return;
            }
        } catch (Exception e) {
            log.error("Could not load values from registry", e);
        }
    }

    public List<MediationStatConfig> load(int tenantId) {
        List<MediationStatConfig> mediationStatConfigList = new ArrayList<>();

        try {
            Registry registry = dasRegistryService.getConfigSystemRegistry(tenantId);
            String serverListPath = MediationDataPublisherConstants.DAS_SERVER_LIST_REG_PATH;
            Resource resource;

            if (registry != null) {
                if (registry.resourceExists(serverListPath)) {
                    resource = registry.get(serverListPath);

                    List<String> idList = resource.getPropertyValues(IDS);

                    if (idList != null) {
                        for (String id : idList) {
                            mediationStatConfigList.add(this.get(id, tenantId));
                        }
                    }

                } else {
                    resource = registry.newResource();
                    resource.setProperty(IDS, new ArrayList<String>());
                    registry.put(serverListPath, resource);
                }

            }


        } catch (Exception e) {
            log.error("Could not load values from registry", e);
        }

        return mediationStatConfigList;
    }

    public MediationStatConfig[] getAllPublisherNames(int tenantId) {
        List<MediationStatConfig> configList = load(tenantId);
        return configList.toArray(new MediationStatConfig[configList.size()]);
    }

    public boolean remove(String serverId, int tenantId) {

        try {
            Registry registry = dasRegistryService.getConfigSystemRegistry(tenantId);
            String resourcePath = MediationDataPublisherConstants.DAS_MEDIATION_MESSAGE_FLOW_REG_PATH + serverId;
            String serverListPath = MediationDataPublisherConstants.DAS_SERVER_LIST_REG_PATH;

            if (registry != null) {
                if (registry.resourceExists(resourcePath)) {
                    registry.delete(resourcePath);
                }

                if (registry.resourceExists(serverListPath)) {
                    Resource listResource = registry.get(serverListPath);
                    List<String> idList = listResource.getPropertyValues(IDS);
                    idList.remove(serverId);
                    listResource.setProperty(IDS, idList);
                    registry.put(serverListPath, listResource);
                }
            } else {
                return false;
            }

        } catch (Exception e) {
            log.error("Could not load values from registry", e);
            return false;
        }

        return true;
    }


    /**
     * Loads configuration from Registry.
     */
 /*   public List<MediationStatConfig> load(int tenantId) {

        List<MediationStatConfig> mediationStatConfigList = new ArrayList<>();

        MediationStatConfig mediationStatConfig = new MediationStatConfig();
        // First set it to defaults, but do not persist
        mediationStatConfig.setMessageFlowTracePublishingEnabled(false);
        mediationStatConfig.setMessageFlowStatsPublishingEnabled(false);
        mediationStatConfig.setUrl(EMPTY_STRING);
        mediationStatConfig.setUserName(EMPTY_STRING);
        mediationStatConfig.setPassword(EMPTY_STRING);
        mediationStatConfig.setProperties(new Property[0]);

        // then load it from registry
        try {

            Registry registry = dasRegistryService.getConfigSystemRegistry(tenantId);

            String url = getConfigurationProperty(DASDataPublisherConstants.DAS_URL,
                                                  registry);
            String userName = getConfigurationProperty(DASDataPublisherConstants.DAS_USER_NAME,
                                                       registry);
            String password = getConfigurationProperty(DASDataPublisherConstants.DAS_PASSWORD,
                                                       registry);
            String streamName = getConfigurationProperty(DASDataPublisherConstants.DAS_STREAM_NAME,
                                                         registry);
            String version = getConfigurationProperty(DASDataPublisherConstants.DAS_VERSION,
                                                      registry);
            String description = getConfigurationProperty(DASDataPublisherConstants.DAS_DESCRIPTION,
                                                          registry);
            String nickName = getConfigurationProperty(DASDataPublisherConstants.DAS_NICK_NAME,
                                                       registry);
            String tracePublishingEnable = getConfigurationProperty(DASDataPublisherConstants.DAS_TRACE_PUBLISHING_ENABLED,
                                                                    registry);
            String statsPublishingEnable = getConfigurationProperty(DASDataPublisherConstants.DAS_STATS_PUBLISHING_ENABLED,
                                                                    registry);

            Properties properties = getAllConfigProperties(MediationDataPublisherConstants.DAS_MEDIATION_STATISTICS_PROPERTIES_REG_PATH,
                                                           registry);

            if (url != null && userName != null && password != null) {

                mediationStatConfig.setMessageFlowTracePublishingEnabled(Boolean.parseBoolean(tracePublishingEnable));
                mediationStatConfig.setMessageFlowStatsPublishingEnabled(Boolean.parseBoolean(statsPublishingEnable));

                mediationStatConfig.setUrl(url);
                mediationStatConfig.setUserName(userName);
                mediationStatConfig.setPassword(password);

                mediationStatConfig.setStreamName(streamName);
                mediationStatConfig.setVersion(version);
                mediationStatConfig.setDescription(description);
                mediationStatConfig.setNickName(nickName);

                if (properties != null) {
                    List<Property> propertyDTOList = new ArrayList<Property>();
                    String[] keys = properties.keySet().toArray(new String[properties.size()]);
                    for (int i = keys.length - 1; i >= 0; i--) {
                        String key = keys[i];
                        Property propertyDTO = new Property();
                        propertyDTO.setKey(key);
                        propertyDTO.setValue(((List<String>) properties.get(key)).get(0));
                        propertyDTOList.add(propertyDTO);
                    }

                    mediationStatConfig.setProperties(propertyDTOList.toArray(new Property[propertyDTOList.size()]));
                }

            } else {
                // Registry does not have eventing config
                update(mediationStatConfig, tenantId);
            }
        } catch (Exception e) {
            log.error("Coul not load values from registry", e);
        }

        mediationStatConfigList.add(mediationStatConfig);

        return mediationStatConfigList;
    }
*/
    private Properties getAllConfigProperties(String mediationStatisticsPropertiesRegPath, Registry registry)
            throws RegistryException {
        Properties properties = null;
        Properties filterProperties = null;

        if (registry.resourceExists(mediationStatisticsPropertiesRegPath)) {
            Resource resource = registry.get(mediationStatisticsPropertiesRegPath);
            properties = resource.getProperties();
            if (properties != null) {
                filterProperties = new Properties();
                for (Map.Entry<Object, Object> keyValuePair : properties.entrySet()) {
                    //When using mounted registry it keeps some properties starting with "registry." we don't need it.
                    if (!keyValuePair.getKey().toString().startsWith(DASDataPublisherConstants.DAS_PREFIX_FOR_REGISTRY_HIDDEN_PROPERTIES)) {
                        filterProperties.put(keyValuePair.getKey(), keyValuePair.getValue());
                    }
                }

            }
        }
        return filterProperties;
    }

    /**
     * Updates all properties of a resource
     *
     * @param properties
     * @param registryPath
     */
    public void updateAllProperties(Properties properties, String registryPath, Registry registry)
            throws RegistryException {
        // Always creating a new resource because properties should be replaced and overridden
        Resource resource = registry.newResource();

        resource.setProperties(properties);
        registry.put(registryPath, resource);
    }


    /**
     * Read the resource from registry
     *
     * @param propertyName
     * @param registry
     * @return
     * @throws RegistryException
     * @throws MediationPublisherException
     */
    /*public String getConfigurationProperty(String propertyName, Registry registry)
            throws RegistryException, MediationPublisherException {
        String resourcePath = MediationDataPublisherConstants.DAS_MEDIATION_STATISTICS_REG_PATH + propertyName;
        String value = null;
        if (registry != null) {
            try {
                if (registry.resourceExists(resourcePath)) {
                    Resource resource = registry.get(resourcePath);
                    value = resource.getProperty(propertyName);
                }
            } catch (Exception e) {
                throw new MediationPublisherException("Error while accessing registry", e);
            }
        }
        return value;
    }*/

    /**
     * Updates the Registry with given config data.
     *
     * @param eventConfig eventing configuration data
     * @param tenantId
     */
/*    public void update(MediationStatConfig eventConfig, int tenantId) {
        try {
            Registry registry = dasRegistryService.getConfigSystemRegistry(tenantId);
            updateConfigProperty(DASDataPublisherConstants.DAS_TRACE_PUBLISHING_ENABLED,
                                 eventConfig.isMessageFlowTracePublishingEnabled(), registry);
            updateConfigProperty(DASDataPublisherConstants.DAS_STATS_PUBLISHING_ENABLED,
                                 eventConfig.isMessageFlowStatsPublishingEnabled(), registry);
            updateConfigProperty(DASDataPublisherConstants.DAS_URL,
                                 eventConfig.getUrl(), registry);
            updateConfigProperty(DASDataPublisherConstants.DAS_USER_NAME,
                                 eventConfig.getUserName(), registry);
            updateConfigProperty(DASDataPublisherConstants.DAS_PASSWORD,
                                 eventConfig.getPassword(), registry);
            updateConfigProperty(DASDataPublisherConstants.DAS_STREAM_NAME,
                                 eventConfig.getStreamName(), registry);
            updateConfigProperty(DASDataPublisherConstants.DAS_VERSION,
                                 eventConfig.getVersion(), registry);
            updateConfigProperty(DASDataPublisherConstants.DAS_NICK_NAME,
                                 eventConfig.getNickName(), registry);
            updateConfigProperty(DASDataPublisherConstants.DAS_DESCRIPTION,
                                 eventConfig.getDescription(), registry);


            Property[] propertiesDTO = eventConfig.getProperties();
            if (propertiesDTO != null) {
                Properties properties = new Properties();
                for (int i = 0; i < propertiesDTO.length; i++) {
                    Property property = propertiesDTO[i];
                    List<String> valueList = new ArrayList<String>();
                    valueList.add(property.getValue());
                    properties.put(property.getKey(), valueList);
                }
                updateAllProperties(properties, MediationDataPublisherConstants.DAS_MEDIATION_STATISTICS_PROPERTIES_REG_PATH,
                                    registry);
            } else {
                updateAllProperties(null, MediationDataPublisherConstants.DAS_MEDIATION_STATISTICS_PROPERTIES_REG_PATH,
                                    registry);
            }

        } catch (Exception e) {
            log.error("Could not update the registry", e);
        }
    }*/

    /**
     * Update the properties
     *
     * @param propertyName
     * @param value
     * @param registry
     * @throws org.wso2.carbon.registry.core.exceptions.RegistryException
     * @throws MediationPublisherException
     */
/*    public void updateConfigProperty(String propertyName, Object value, Registry registry)
            throws RegistryException, MediationPublisherException {
//        String resourcePath = MediationDataPublisherConstants.DAS_MEDIATION_STATISTICS_REG_PATH + propertyName;
        Resource resource;
        if (registry != null) {
            try {
                if (!registry.resourceExists(resourcePath)) {
                    resource = registry.newResource();
                    resource.addProperty(propertyName, value.toString());
                    registry.put(resourcePath, resource);
                } else {
                    resource = registry.get(resourcePath);
                    resource.setProperty(propertyName, value.toString());
                    registry.put(resourcePath, resource);
                }
            } catch (Exception e) {
                throw new MediationPublisherException("Error while accessing registry", e);
            }
        }
    }*/


}
