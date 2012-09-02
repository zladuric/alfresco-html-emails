package org.alfresco.repo.jscript;

import java.util.ArrayList;
import java.util.Collections;

import org.alfresco.repo.action.executer.HtmlEmailsActionExecuter;
import org.alfresco.repo.jscript.BaseScopableProcessorExtension;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ActionService;
import org.alfresco.service.cmr.repository.NodeRef;

public class HtmlEmails extends BaseScopableProcessorExtension
{
    private ServiceRegistry serviceRegistry;
/*
 * @method sendMails
 * @param recipient String - if single recipient is set, this is used
 * @param recipients String - param to send to multiple persons
 * @param subject String - subject line
 * @param text String - html text
 * @param templateNode - nodeRef if sending from template
 * @param imageList to add inline images
 * @param imagesfolder - nodeRef of the folder containing images and other inline conten
 * @param String attchmentsFolderNode - folder node, if it has children, those will be added as attachments
 *  */
    public void sendMails(String recipient, String recipients, String subject, String text, String templateNode, String attchmentsFolderNode, String imagesList, String imagesFolder)
    {
        NodeRef attachmentsNodeRef = new NodeRef(attchmentsFolderNode);
        NodeRef templateNodeRef = new NodeRef(templateNode);
        ActionService actionService = serviceRegistry.getActionService();

        Action mail = actionService.createAction(HtmlEmailsActionExecuter.NAME);

        ArrayList<String> recipientsList = new ArrayList<String>();
        Collections.addAll(recipientsList, recipients.split(","));
        mail.setParameterValue(HtmlEmailsActionExecuter.PARAM_TO, recipient);
        mail.setParameterValue(HtmlEmailsActionExecuter.PARAM_TO_MANY, recipientsList);
        mail.setParameterValue(HtmlEmailsActionExecuter.PARAM_SUBJECT, subject);
        mail.setParameterValue(HtmlEmailsActionExecuter.PARAM_TEXT, text);
        mail.setParameterValue(HtmlEmailsActionExecuter.PARAM_TEMPLATE, templateNodeRef);
        mail.setParameterValue(HtmlEmailsActionExecuter.PARAM_ATTCHMENTS_FOLDER, attachmentsNodeRef);
        mail.setParameterValue(HtmlEmailsActionExecuter.PARAM_IMAGES, imagesList);        
        mail.setParameterValue(HtmlEmailsActionExecuter.PARAM_IMAGES_FOLDER, attachmentsNodeRef);
        actionService.executeAction(mail, attachmentsNodeRef, false, false);
    }

    public void setServiceRegistry(ServiceRegistry serviceRegistry)
    {
        this.serviceRegistry = serviceRegistry;
    }

}