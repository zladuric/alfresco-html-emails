package org.alfresco.repo.action.executer;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.mail.internet.MimeMessage;
import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.action.ParameterDefinitionImpl;
import org.alfresco.repo.template.DateCompareMethod;
import org.alfresco.repo.template.HasAspectMethod;
import org.alfresco.repo.template.I18NMessageMethod;
import org.alfresco.repo.template.TemplateNode;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ParameterDefinition;
import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.TemplateService;
import org.alfresco.service.cmr.security.AuthenticationService;
import org.alfresco.service.cmr.security.PersonService;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.validator.EmailValidator;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

@SuppressWarnings("deprecation")
public class HtmlEmailsActionExecuter extends ActionExecuterAbstractBase implements InitializingBean
{

    private static Log logger = LogFactory.getLog(HtmlEmailsActionExecuter.class);

    /**
     * Action executor constants
     */
    public static final String NAME = "mailAsHtml";
    public static final String PARAM_TO = "to";
    public static final String PARAM_TO_MANY = "to_many";
    public static final String PARAM_SUBJECT = "subject";
    public static final String PARAM_TEXT = "text";
    public static final String PARAM_FROM = "from";
    public static final String PARAM_TEMPLATE = "template";
    public static final String PARAM_ATTCHMENTS_FOLDER = "attachments_folder";
    public static final String PARAM_IMAGES = "images";
    public static final String PARAM_IMAGES_FOLDER = "images_folder";
    public static final String PARAM_ADDITIONAL_INFO = "additional_info";
    

    /**
     * From address
     */
    private static final String FROM_ADDRESS = "zduric@myoffice.hr";

    private static final String REPO_REMOTE_URL = "http://localhost:8888/alfresco";

    /**
     * The java mail sender
     */
    private JavaMailSender javaMailSender;

    /**
     * The Template servicetoManyMails
     */
    private TemplateService templateService;

    /**
     * The Person service
     */
    private PersonService personService;

    /**
     * The Authentication service
     */
    private AuthenticationService authService;

    /**
     * The Node Service
     */
    private NodeService nodeService;

    /**
     * The Service registry
     */
    private ServiceRegistry serviceRegistry;

    /**
     * Default from address
     */
    private String fromAddress = null;

    /**
     * Default alfresco installation url
     */
    private String repoRemoteUrl = null;

    /**
     * @param javaMailSender
     *            the java mail sender
     */
    public void setMailService(JavaMailSender javaMailSender)
    {
        this.javaMailSender = javaMailSender;
    }

    /**
     * @param templateService
     *            the TemplateService
     */
    public void setTemplateService(TemplateService templateService)
    {
        this.templateService = templateService;
    }

    /**
     * @param personService
     *            the PersonService
     */
    public void setPersonService(PersonService personService)
    {
        this.personService = personService;
    }

    /**
     * @param authService
     *            the AuthenticationService
     */
    public void setAuthenticationService(AuthenticationService authService)
    {
        this.authService = authService;
    }

    /**
     * @param serviceRegistry
     *            the ServiceRegistry
     */
    public void setServiceRegistry(ServiceRegistry serviceRegistry)
    {
        this.serviceRegistry = serviceRegistry;
    }

    /**
     * @param nodeService
     *            the NodeService to set.
     */
    public void setNodeService(NodeService nodeService)
    {
        this.nodeService = nodeService;
    }

    /**
     * @param fromAddress
     *            The default mail address.
     */
    public void setFromAddress(String fromAddress)
    {
        this.fromAddress = fromAddress;
    }

    /**
     * 
     * @param repoRemoteUrl
     *            The default alfresco installation url
     */
    public void setRepoRemoteUrl(String repoRemoteUrl)
    {
        this.repoRemoteUrl = repoRemoteUrl;
    }

    /**
     * Initialise bean
     */
    public void afterPropertiesSet() throws Exception
    {
        if (fromAddress == null || fromAddress.length() == 0)
        {
            fromAddress = FROM_ADDRESS;
        }

        if (repoRemoteUrl == null || repoRemoteUrl.length() == 0)
        {
            repoRemoteUrl = REPO_REMOTE_URL;
        }

    }

    /**
     * Send an email message
     * 
     * @throws AlfrescoRuntimeExeption
     */
    @SuppressWarnings("unchecked")
    @Override
    protected void executeImpl(final Action ruleAction, final NodeRef actionedUponNodeRef)
    {
        try
        {
            MimeMessage message = javaMailSender.createMimeMessage();
            // use the true flag to indicate you need a multipart message
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            // set recipient
            String to = (String) ruleAction.getParameterValue(PARAM_TO);
            if (to != null && to.length() != 0)
            {
                helper.setTo(to);
            } 
            else
            {
                // see if multiple recipients have been supplied - as a list of
                // authorities
                Serializable toManyMails = ruleAction.getParameterValue(PARAM_TO_MANY);
                List<String> recipients = new ArrayList<String>();
                if (toManyMails instanceof List)
                {
                    for (String mailAdress : (List<String>) toManyMails)
                    {
                        if (validateAddress(mailAdress))
                        {
                            recipients.add(mailAdress);
                        }
                    }
                } 
                else if (toManyMails instanceof String)
                {
                    if (validateAddress((String) toManyMails))
                    {
                        recipients.add((String) toManyMails);
                    }
                }
                if (recipients != null && recipients.size() > 0)
                {
                    helper.setTo(recipients.toArray(new String[recipients.size()]));
                } 
                else
                {
                    // No recipients have been specified
                    logger.error("No recipient has been specified for the mail action");
                }
            }

            // set subject line
            helper.setSubject((String) ruleAction.getParameterValue(PARAM_SUBJECT));

            // See if an email template has been specified
            String text = null;
            NodeRef templateRef = (NodeRef) ruleAction.getParameterValue(PARAM_TEMPLATE);
            if (templateRef != null)
            {
                // build the email template model
                Map<String, Object> model = createEmailTemplateModel(actionedUponNodeRef, ruleAction);

                // process the template against the model
                text = templateService.processTemplate("freemarker", templateRef.toString(), model);
            }

            // set the text body of the message
            if (text == null)
            {
                text = (String) ruleAction.getParameterValue(PARAM_TEXT);
            }
            // adding the boolean true to send as HTML
            helper.setText(text, true);
            FileFolderService fileFolderService = serviceRegistry.getFileFolderService();
            /* add inline images. 
             * "action.parameters.images is a ,-delimited string, containing a map of images and resources, from this example:  
            message.setText("my text <img src='cid:myLogo'>", true);
            message.addInline("myLogo", new ClassPathResource("img/mylogo.gif"));
            so the "images" param can look like this: headerLogo|images/headerLogoNodeRef,footerLogo|footerLogoNodeRef 
             */
            String imageList = (String) ruleAction.getParameterValue(PARAM_IMAGES);
            System.out.println(imageList);
            String[] imageMap = imageList.split(","); // comma no spaces
            Map<String, String> images = new HashMap<String, String>();
            for (String image : imageMap) {
            	System.out.println(image);
            	String map[] = image.split("\\|");
            	for (String key: map) {
            		System.out.println(key);
            		
            	}
            	
            	System.out.println(map.length);
            	
            	images.put(map[0].trim(), map[1].trim());
            	System.out.println(images.size());
            	System.out.println("-"+map[0]+" "+map[1]+"-");
            }
            NodeRef imagesFolderNodeRef = (NodeRef) ruleAction.getParameterValue(PARAM_IMAGES_FOLDER);
            if (null != imagesFolderNodeRef) {
            	 ContentService contentService = serviceRegistry.getContentService();
            	 System.out.println("mapping");
            	for (Map.Entry<String, String> entry : images.entrySet())
            	{
            		System.out.println(entry.getKey() + " " + entry.getValue() + " " + ruleAction.getParameterValue(PARAM_IMAGES_FOLDER));
            		NodeRef imageFile = fileFolderService.searchSimple(imagesFolderNodeRef, entry.getValue());
            		if (null != imageFile) {
            		       ContentReader reader = contentService.getReader(imageFile, ContentModel.PROP_CONTENT);
            		       ByteArrayResource resource = new ByteArrayResource(IOUtils.toByteArray(reader.getContentInputStream()));
            		       helper.addInline(entry.getKey(), resource, reader.getMimetype());
            		      
            		} else {
            			logger.error("No image for " + entry.getKey());
            		}
            	}
            } else {
            	logger.error("No images folder");
            }
            
            
            // set the from address
            NodeRef person = personService.getPerson(authService.getCurrentUserName());

            String fromActualUser = null;
            if (person != null)
            {
                fromActualUser = (String) nodeService.getProperty(person, ContentModel.PROP_EMAIL);
            }
            if (fromActualUser != null && fromActualUser.length() != 0)
            {
                helper.setFrom(fromActualUser);
            } 
            else
            {
                String from = (String) ruleAction.getParameterValue(PARAM_FROM);
                if (from == null || from.length() == 0)
                {
                    helper.setFrom(fromAddress);
                } 
                else
                {
                    helper.setFrom(from);
                }
            }
            NodeRef attachmentsFolder = (NodeRef) ruleAction.getParameterValue(PARAM_ATTCHMENTS_FOLDER);
            if (attachmentsFolder != null)
            {
                
                List<FileInfo> attachFiles = fileFolderService.listFiles(attachmentsFolder);
                if (attachFiles != null && attachFiles.size() > 0)
                {
                    for (FileInfo attachFile : attachFiles)
                    {
                        ContentReader contentReader = fileFolderService.getReader(attachFile.getNodeRef());
                        ByteArrayResource resource = new ByteArrayResource(IOUtils.toByteArray(contentReader.getContentInputStream()));
                        helper.addAttachment(attachFile.getName(), resource, contentReader.getMimetype());
                    }
                }
            }

            // Send the message unless we are in "testMode"
            javaMailSender.send(message);
        } 
        catch (Exception e)
        {
            String toUser = (String) ruleAction.getParameterValue(PARAM_TO);
            if (toUser == null)
            {
                Object obj = ruleAction.getParameterValue(PARAM_TO_MANY);
                if (obj != null)
                {
                    toUser = obj.toString();
                }
            }

            logger.error("Failed to send email to " + toUser, e);

            throw new AlfrescoRuntimeException("Failed to send email to:" + toUser, e);
        }
    }

    /**
     * Return true if address has valid format
     * 
     * @param address
     * 
     * @return
     */
    private boolean validateAddress(String address)
    {
        boolean result = false;

        EmailValidator emailValidator = EmailValidator.getInstance();
        if (emailValidator.isValid(address))
        {
            result = true;
        } 
        else
        {
            logger.error("Failed to send email to '" + address + "' as the address is incorrectly formatted");
        }

        return result;
    }

    /**
     * @param ref
     *            The node representing the current document ref
     * 
     * @return Model map for email templates
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> createEmailTemplateModel(NodeRef ref, Action ruleAction)
    {
        Map<String, Object> model = new HashMap<String, Object>();

        NodeRef person = personService.getPerson(authService.getCurrentUserName());
        model.put("person", new TemplateNode(person, serviceRegistry, null));
        model.put("document", new TemplateNode(ref, serviceRegistry, null));
        NodeRef parent = serviceRegistry.getNodeService().getPrimaryParent(ref).getParentRef();
        model.put("space", new TemplateNode(parent, serviceRegistry, null));

        // current date/time is useful to have and isn't supplied by FreeMarker
        // by default
        model.put("date", new Date());

        // add custom method objects
        model.put("hasAspect", new HasAspectMethod());
        model.put("message", new I18NMessageMethod());
        model.put("dateCompare", new DateCompareMethod());
        model.put("url", new URLHelper(repoRemoteUrl));

        Map<String, Object> additionalParams = (Map<String, Object>) ruleAction.getParameterValue(PARAM_ADDITIONAL_INFO);

        if (additionalParams != null)
        {
            model.putAll(additionalParams);
        }

        return model;
    }

    /**
     * Add the parameter definitions
     */
    @Override
    protected void addParameterDefinitions(List<ParameterDefinition> paramList)
    {
        paramList.add(new ParameterDefinitionImpl(PARAM_TO, DataTypeDefinition.TEXT, false, getParamDisplayLabel(PARAM_TO)));
        paramList.add(new ParameterDefinitionImpl(PARAM_TO_MANY, DataTypeDefinition.ANY, false, getParamDisplayLabel(PARAM_TO_MANY), true));
        paramList.add(new ParameterDefinitionImpl(PARAM_SUBJECT, DataTypeDefinition.TEXT, true, getParamDisplayLabel(PARAM_SUBJECT)));
        paramList.add(new ParameterDefinitionImpl(PARAM_TEXT, DataTypeDefinition.TEXT, false, getParamDisplayLabel(PARAM_TEXT)));
        paramList.add(new ParameterDefinitionImpl(PARAM_FROM, DataTypeDefinition.TEXT, false, getParamDisplayLabel(PARAM_FROM)));
        paramList.add(new ParameterDefinitionImpl(PARAM_TEMPLATE, DataTypeDefinition.NODE_REF, false, getParamDisplayLabel(PARAM_TEMPLATE), false, "ac-email-templates"));
        paramList.add(new ParameterDefinitionImpl(PARAM_ATTCHMENTS_FOLDER, DataTypeDefinition.NODE_REF, false, getParamDisplayLabel(PARAM_ATTCHMENTS_FOLDER)));
        paramList.add(new ParameterDefinitionImpl(PARAM_ADDITIONAL_INFO, DataTypeDefinition.ANY, false, getParamDisplayLabel(PARAM_ADDITIONAL_INFO)));
    }

    public static class URLHelper
    {
        String contextPath;
        String serverPath;

        public URLHelper(String repoRemoteUrl)
        {
            String[] parts = repoRemoteUrl.split("/");
            this.contextPath = "/" + parts[parts.length - 1];
            this.serverPath = parts[0] + "//" + parts[2];
        }

        public String getContext()
        {
            return this.contextPath;
        }

        public String getServerPath()
        {
            return this.serverPath;
        }
    }
}