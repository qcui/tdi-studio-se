// ============================================================================
//
// Copyright (C) 2006-2007 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.repository.ui.wizards.context;

import org.apache.log4j.Logger;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWizard;
import org.talend.commons.exception.PersistenceException;
import org.talend.commons.ui.image.ImageProvider;
import org.talend.commons.ui.swt.dialogs.ErrorDialogWidthDetailArea;
import org.talend.commons.utils.VersionUtils;
import org.talend.core.CorePlugin;
import org.talend.core.context.Context;
import org.talend.core.context.RepositoryContext;
import org.talend.core.model.context.JobContextManager;
import org.talend.core.model.process.IContextManager;
import org.talend.core.model.properties.ContextItem;
import org.talend.core.model.properties.PropertiesFactory;
import org.talend.core.model.properties.Property;
import org.talend.core.model.repository.ERepositoryObjectType;
import org.talend.core.model.repository.RepositoryObject;
import org.talend.core.ui.images.ECoreImage;
import org.talend.repository.RepositoryPlugin;
import org.talend.repository.i18n.Messages;
import org.talend.repository.model.IProxyRepositoryFactory;
import org.talend.repository.model.ProxyRepositoryFactory;
import org.talend.repository.model.RepositoryNode;
import org.talend.repository.model.RepositoryNodeUtilities;
import org.talend.repository.model.RepositoryNode.ENodeType;
import org.talend.repository.ui.wizards.PropertiesWizardPage;
import org.talend.repository.ui.wizards.RepositoryWizard;
import org.talend.repository.ui.wizards.metadata.connection.Step0WizardPage;

/**
 * FileWizard present the FileForm. Use to create a new connection to a DB.
 */

public class ContextWizard extends RepositoryWizard implements INewWizard {

    private static Logger log = Logger.getLogger(ContextWizard.class);

    private PropertiesWizardPage contextWizardPage0;

    private Property contextProperty;

    private ContextItem contextItem;

    private IContextManager contextManager;

    /**
     * Constructor for FileWizard.
     * 
     * @param workbench
     * @param selection
     * @param strings
     */
    @SuppressWarnings("unchecked")//$NON-NLS-1$
    public ContextWizard(IWorkbench workbench, boolean creation, ISelection selection, boolean forceReadOnly) {
        super(workbench, creation, forceReadOnly);
        pathToSave = getPath(selection);

        setWindowTitle(""); //$NON-NLS-1$
        setDefaultPageImageDescriptor(ImageProvider.getImageDesc(ECoreImage.CONTEXT_WIZ));
        setNeedsProgressMonitor(true);

        if (creation) {
            contextItem = PropertiesFactory.eINSTANCE.createContextItem();
            contextProperty = PropertiesFactory.eINSTANCE.createProperty();
            contextProperty.setAuthor(((RepositoryContext) CorePlugin.getContext().getProperty(
                    Context.REPOSITORY_CONTEXT_KEY)).getUser());
            contextProperty.setVersion(VersionUtils.DEFAULT_VERSION);
            contextProperty.setStatusCode(""); //$NON-NLS-1$

            contextItem.setProperty(contextProperty);

            contextManager = new JobContextManager();
        } else {
            RepositoryNode node = (RepositoryNode) ((IStructuredSelection) selection).getFirstElement();
            RepositoryObject object = (RepositoryObject) node.getObject();
            setRepositoryObject(object);
            isRepositoryObjectEditable();
            initLockStrategy();

            contextItem = (ContextItem) object.getProperty().getItem();
            contextProperty = contextItem.getProperty();
            contextManager = new JobContextManager(contextItem.getContext(), contextItem.getDefaultContext());
        }
        initLockStrategy();
    }

    /**
     * Adding the page to the wizard.
     */
    public void addPages() {
        setWindowTitle(Messages.getString("ContextWizard.Title")); //$NON-NLS-1$
        contextWizardPage0 = new Step0WizardPage(contextProperty, pathToSave, ERepositoryObjectType.CONTEXT,
                !isRepositoryObjectEditable(), creation);
        contextWizardPage0.setTitle(Messages.getString("ContextWizard.step0Title")); //$NON-NLS-1$
        contextWizardPage0.setDescription(Messages.getString("ContextWizard.step0Description")); //$NON-NLS-1$
        addPage(contextWizardPage0);
        if (creation) {
            contextWizardPage0.setPageComplete(false);
        }

        ContextPage contextPage = new ContextPage("test", contextManager, !isRepositoryObjectEditable()); //$NON-NLS-1$
        contextPage.setTitle(Messages.getString("ContextWizard.contextPageTitle")); //$NON-NLS-1$
        contextPage.setDescription(Messages.getString("ContextWizard.contextPageDescription")); //$NON-NLS-1$
        addPage(contextPage);
    }

    /**
     * This method determine if the 'Finish' button is enable This method is called when 'Finish' button is pressed in
     * the wizard. We will create an operation and run it using wizard as execution context.
     */
    public boolean performFinish() {

        boolean formIsPerformed = contextManager.getListContext().size() != 0;
        // if (delimitedFileWizardPage3 == null) {
        // formIsPerformed = delimitedFileWizardPage2.isPageComplete();
        // } else {
        // formIsPerformed = delimitedFileWizardPage3.isPageComplete();
        // }

        if (formIsPerformed) {
            IProxyRepositoryFactory factory = ProxyRepositoryFactory.getInstance();
            try {
                if (creation) {
                    String nextId = factory.getNextId();
                    contextProperty.setId(nextId);
                    contextManager.saveToEmf(contextItem.getContext());
                    contextItem.setDefaultContext(contextManager.getDefaultContext().getName());
                    factory.create(contextItem, contextWizardPage0.getDestinationPath());
                } else {
                    contextItem.getContext().clear();
                    contextManager.saveToEmf(contextItem.getContext());
                    contextItem.setDefaultContext(contextManager.getDefaultContext().getName());
                    factory.save(contextItem);

                    updateRelatedView();

                }
                closeLockStrategy();
            } catch (PersistenceException e) {
                String detailError = e.toString();
                new ErrorDialogWidthDetailArea(getShell(), PID,
                        Messages.getString("CommonWizard.persistenceException"), //$NON-NLS-1$
                        detailError);
                log.error(Messages.getString("CommonWizard.persistenceException") + "\n" + detailError); //$NON-NLS-1$ //$NON-NLS-2$
                return false;
            }
            return true;
        } else {
            return false;
        }
    }

    /**
     * DOC bqian Comment method "updateProcessContextManager".
     */
    private void updateRelatedView() {
        RepositoryPlugin.getDefault().getDesignerCoreService().switchToCurContextsView();
    }

    /**
     * We will accept the selection in the workbench to see if we can initialize from it.
     * 
     * @see IWorkbenchWizard#init(IWorkbench, IStructuredSelection)
     */
    public void init(final IWorkbench workbench, final IStructuredSelection selection2) {
        this.selection = selection2;
    }

    private IPath getPath(ISelection selection) {
        RepositoryNode node = (RepositoryNode) ((IStructuredSelection) selection).getFirstElement();

        IPath path;
        if (node.getType() == ENodeType.SIMPLE_FOLDER || node.getType() == ENodeType.SYSTEM_FOLDER) {
            path = RepositoryNodeUtilities.getPath(node);
        } else {
            path = new Path(""); //$NON-NLS-1$
        }
        return path;
    }

}
