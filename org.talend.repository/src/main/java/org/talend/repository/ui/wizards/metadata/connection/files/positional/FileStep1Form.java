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
package org.talend.repository.ui.wizards.metadata.connection.files.positional;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

import org.apache.log4j.Logger;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Listener;
import org.talend.commons.ui.swt.formtools.Form;
import org.talend.commons.ui.swt.formtools.LabelledCombo;
import org.talend.commons.ui.swt.formtools.LabelledFileField;
import org.talend.commons.ui.swt.formtools.LabelledText;
import org.talend.commons.ui.swt.formtools.UtilsButton;
import org.talend.commons.ui.utils.PathUtils;
import org.talend.commons.utils.encoding.CharsetToolkit;
import org.talend.core.model.metadata.builder.connection.FileFormat;
import org.talend.core.model.metadata.builder.connection.RowSeparator;
import org.talend.core.model.properties.ConnectionItem;
import org.talend.core.model.utils.TalendTextUtils;
import org.talend.repository.i18n.Messages;
import org.talend.repository.ui.swt.filepositionalviewer.FilePositionalViewer;
import org.talend.repository.ui.swt.filepositionalviewer.GraphicRule;
import org.talend.repository.ui.swt.utils.AbstractPositionalFileStepForm;

/**
 * @author ocarbone
 * 
 */
public class FileStep1Form extends AbstractPositionalFileStepForm {

    private static Logger log = Logger.getLogger(FileStep1Form.class);

    /**
     * Settings.
     */
    private static final int WIDTH_GRIDDATA_PIXEL = 300;

    /**
     * Main Fields.
     */
    private LabelledCombo serverCombo;

    private LabelledFileField fileField;

    private LabelledCombo fileFormatCombo;

    /**
     * Another.
     */
    private boolean filePathIsDone;

    private static FilePositionalViewer filePositionalViewer;

    private UtilsButton cancelButton;

    private ScrolledComposite scrolledCompositeFileViewer;

    private GraphicRule graphicRule;

    private LabelledText fieldPositionText;

    private boolean readOnly;

    private static LabelledText fieldSeparatorText;

    /**
     * Constructor to use by RCP Wizard.
     * 
     * @param existingNames
     * 
     * @param Composite
     * @param Wizard
     * @param Style
     */
    public FileStep1Form(Composite parent, ConnectionItem connectionItem, String[] existingNames) {
        super(parent, connectionItem, existingNames);
        setupForm();
    }

    /**
     * 
     * Initialize value, forceFocus first field.
     */
    @Override
    protected void initialize() {

        // Initialize the fields
        String value = null;
        fileFormatCombo.setText(getConnection().getFormat().getName());
        fileFormatCombo.clearSelection();

        value = getConnection().getServer();
        if (value == null) {
            serverCombo.select(0);
            getConnection().setServer(serverCombo.getText());
        } else {
            serverCombo.setText(value);
        }
        serverCombo.clearSelection();

        // Just mask it.
        serverCombo.setReadOnly(true);

        fileField.setText(getConnection().getFilePath());

        value = getConnection().getFieldSeparatorValue();

        // remove quotes.
        if (!value.equals("*")) {
            value = value.substring(1, value.length() - 1);
        }
        checkFilePathAndManageIt(false);

        // update the field Separator
        fieldSeparatorText.setText(value);

        // update the positionalViewer
        filePositionalViewer.setSeparatorValue(value, true);
        // update the field Position
        fieldPositionText.setText(filePositionalViewer.calculatePositionX());
    }

    /**
     * DOC ocarbone Comment method "adaptFormToReadOnly".
     */
    @Override
    protected void adaptFormToReadOnly() {
        readOnly = isReadOnly();
        // serverCombo.setReadOnly(isReadOnly());
        fieldSeparatorText.setReadOnly(isReadOnly());
        filePositionalViewer.setEnabled(!isReadOnly());
        fieldPositionText.setReadOnly(isReadOnly());
        fileField.setReadOnly(isReadOnly());
        fileFormatCombo.setReadOnly(isReadOnly());
    }

    @Override
    protected void addFields() {
        int heightViewer = 150;

        // Group File Location
        Group groupFileViewer = Form.createGroup(this, 1, Messages.getString("FileStep1.groupFileLocationSettings"), 95); //$NON-NLS-1$
        Composite compositeFileLocation = Form.startNewDimensionnedGridLayout(groupFileViewer, 3, WIDTH_GRIDDATA_PIXEL, 95);

        // server Combo
        String[] serverLocation = { "Localhost 127.0.0.1" }; //$NON-NLS-1$
        serverCombo = new LabelledCombo(compositeFileLocation, Messages.getString("FileStep1.server"), Messages //$NON-NLS-1$
                .getString("FileStep1.serverTip"), serverLocation, 2, true, SWT.NONE); //$NON-NLS-1$

        // file Field
        String[] extensions = { "*.txt", "*.*", "*" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        fileField = new LabelledFileField(compositeFileLocation, Messages.getString("FileStep1.filepath"), extensions); //$NON-NLS-1$

        // file format Combo
        String[] fileFormat = { FileFormat.WINDOWS_LITERAL.getName(), FileFormat.UNIX_LITERAL.getName(),
                FileFormat.MAC_LITERAL.getName() };
        fileFormatCombo = new LabelledCombo(compositeFileLocation, Messages.getString("FileStep1.format"), Messages //$NON-NLS-1$
                .getString("FileStep1.formatTip"), fileFormat, 2); //$NON-NLS-1$

        // Group File Viewer
        groupFileViewer = Form.createGroup(this, 1, Messages.getString("FileStep1.groupFileViewer"), heightViewer + 80); //$NON-NLS-1$

        Composite compositeBorderFileViewer = new Composite(groupFileViewer, SWT.BORDER);
        compositeBorderFileViewer.setLayout(new GridLayout());
        GridData gridData = new GridData(GridData.FILL_BOTH);
        gridData.minimumHeight = heightViewer + 25;
        gridData.heightHint = heightViewer + 25;
        compositeBorderFileViewer.setLayoutData(gridData);

        graphicRule = new GraphicRule(compositeBorderFileViewer, SWT.NONE);

        scrolledCompositeFileViewer = new ScrolledComposite(compositeBorderFileViewer, SWT.H_SCROLL | SWT.V_SCROLL | SWT.NONE);
        scrolledCompositeFileViewer.setExpandHorizontal(true);
        scrolledCompositeFileViewer.setExpandVertical(true);
        GridData gridData1 = new GridData(GridData.FILL_BOTH);
        gridData1.widthHint = WIDTH_GRIDDATA_PIXEL;
        gridData1.heightHint = heightViewer;
        gridData1.horizontalSpan = 2;
        scrolledCompositeFileViewer.setLayoutData(gridData1);

        filePositionalViewer = new FilePositionalViewer(scrolledCompositeFileViewer, SWT.LEFT);
        graphicRule.moveAbove(filePositionalViewer);
        graphicRule.setPositionalViewer(filePositionalViewer);

        scrolledCompositeFileViewer.setContent(filePositionalViewer);
        scrolledCompositeFileViewer.setMinSize(filePositionalViewer.computeSize(SWT.DEFAULT, SWT.DEFAULT));

        // Field fieldSeparatorText
        Composite compositeFieldSeparator = Form.startNewDimensionnedGridLayout(groupFileViewer, 2, WIDTH_GRIDDATA_PIXEL, 50);
        fieldSeparatorText = new LabelledText(compositeFieldSeparator, Messages.getString("FileStep2.fieldSeparator"), 1, true, //$NON-NLS-1$
                SWT.RIGHT);
        fieldSeparatorText.setToolTipText(Messages.getString("FileStep2.fieldSeparatorPositionalTip")); //$NON-NLS-1$

        fieldPositionText = new LabelledText(compositeFieldSeparator, Messages.getString("FileStep2.fieldPosition"), 1, true, //$NON-NLS-1$
                SWT.RIGHT);
        fieldPositionText.setToolTipText(Messages.getString("FileStep2.fieldPositionTip")); //$NON-NLS-1$

        if (!isInWizard()) {
            // Composite BottomButton
            Composite compositeBottomButton = Form.startNewGridLayout(this, 2, false, SWT.CENTER, SWT.CENTER);
            // Button Cancel
            cancelButton = new UtilsButton(compositeBottomButton, Messages.getString("CommonWizard.Cancel"), WIDTH_BUTTON_PIXEL, //$NON-NLS-1$
                    HEIGHT_BUTTON_PIXEL);
            // nextButton = new UtilsButton(compositeBottomButton, "Next", WIDTH_BUTTON_PIXEL, HEIGHT_BUTTON_PIXEL);
        }
        addUtilsButtonListeners();
    }

    @Override
    protected void addUtilsButtonListeners() {

        if (!isInWizard()) {
            // Event cancelButton
            cancelButton.addSelectionListener(new SelectionAdapter() {

                @Override
                public void widgetSelected(final SelectionEvent e) {
                    getShell().close();
                }
            });
            // // Event nextButton
            // nextButton.addSelectionListener(new SelectionAdapter() {
            // public void widgetSelected(final SelectionEvent e) {
            // }
            // });
        }
    }

    /**
     * Main Fields addControls.
     */
    @Override
    protected void addFieldsListeners() {

        // Synchronise the graphicRule width the scrolledCompositeFileViewer
        scrolledCompositeFileViewer.getHorizontalBar().addListener(SWT.Selection, new Listener() {

            public void handleEvent(Event e) {
                int hSelection = scrolledCompositeFileViewer.getHorizontalBar().getSelection();
                graphicRule.setBounds(5 - hSelection, 5, filePositionalViewer.getText().getSize().x, graphicRule.getSize().y);
            }
        });
        scrolledCompositeFileViewer.addControlListener(new ControlListener() {

            public void controlMoved(ControlEvent e) {
            }

            public void controlResized(ControlEvent e) {
                int compositeXsize = filePositionalViewer.getText().getSize().x;
                int compositeYsize = graphicRule.getSize().y;
                if (compositeXsize <= 10000) {
                    graphicRule.setBounds(5, 5, 500, 0);
                    graphicRule.setSize(10000, compositeYsize);
                } else {
                    graphicRule.setBounds(5, 5, 500, 0);
                    graphicRule.setSize(compositeXsize, compositeYsize);
                }
            }
        });

        // Event serverCombo
        serverCombo.addModifyListener(new ModifyListener() {

            public void modifyText(final ModifyEvent e) {
                getConnection().setServer(serverCombo.getText());
                checkFieldsValue();
            }
        });

        // fileField : Event modifyText
        fileField.addModifyListener(new ModifyListener() {

            public void modifyText(final ModifyEvent e) {
                getConnection().setFilePath(PathUtils.getPortablePath(fileField.getText()));
                checkFilePathAndManageIt(true);
            }
        });

        // Event fileFormatCombo
        fileFormatCombo.addModifyListener(new ModifyListener() {

            // Event Modify
            public void modifyText(final ModifyEvent e) {
                getConnection().setFormat(FileFormat.getByName(fileFormatCombo.getText()));
                // if necessary, adapt the rowSeparator to the file format
                if (getConnection().getRowSeparatorType() == RowSeparator.STANDART_EOL_LITERAL) {
                    if (getConnection().getFormat().toString().equals(FileFormat.MAC_LITERAL.getName())) {
                        getConnection().setRowSeparatorValue("\\r"); //$NON-NLS-1$
                    } else {
                        getConnection().setRowSeparatorValue("\\n"); //$NON-NLS-1$
                    }
                }
                checkFilePathAndManageIt(false);
            }
        });

        // when positionalViewer is modified : synchronise the field SeparatorText
        filePositionalViewer.getFieldSeparatorValue().addModifyListener(new ModifyListener() {

            public void modifyText(ModifyEvent e) {

                String value = filePositionalViewer.getFieldSeparatorValue().getText();
                String valueToField = value;
                if (fieldSeparatorText.getText().contains("*")) { //$NON-NLS-1$
                    if (value.equals("")) { //$NON-NLS-1$
                        valueToField = "*"; //$NON-NLS-1$
                    } else {
                        valueToField = value + ",*"; //$NON-NLS-1$
                    }
                }

                if (!fieldSeparatorText.getText().equals(valueToField)) {
                    // update the field separator Text
                    fieldSeparatorText.setEditable(false);
                    fieldSeparatorText.setText(valueToField);
                    fieldSeparatorText.setEditable(true);
                    // update the field position Text
                    fieldPositionText.setEditable(false);
                    fieldPositionText.setText(filePositionalViewer.calculatePositionX());
                    fieldPositionText.setEditable(true);
                    checkFieldsValue();
                }
            }
        });

        // Separator Text : check the value and synchronise the positionalViewer
        fieldSeparatorText.addModifyListener(new ModifyListener() {

            public void modifyText(final ModifyEvent e) {
                // update the connection
                getConnection().setFieldSeparatorValue(fieldSeparatorText.getText());

                if (fieldSeparatorText.getEditable()
                        && getConnection().getFieldSeparatorValue().equals(fieldSeparatorText.getText())) {

                    // check the value and display the status Error is needed
                    if (!checkFieldSeparatorValue()) {
                        // the value isn't correct => clean markers of the positionalViewer
                        fieldPositionText.setEditable(false);
                        if (filePositionalViewer.getVisible()) {
                            filePositionalViewer.cleanAllMarkers();
                            filePositionalViewer.setEnabled(false);
                            graphicRule.setEnabled(false);
                        }
                    } else {
                        String value = getValidateFieldSeparator(fieldSeparatorText.getText());
                        Point selection = fieldSeparatorText.getSelection();
                        if ((!value.equals(getConnection().getFieldSeparatorValue()))) {
                            // the value isn't correct => clean markers of the positionalViewer
                            fieldPositionText.setEditable(false);
                            if (filePositionalViewer.getVisible()) {
                                filePositionalViewer.setEnabled(false);
                                graphicRule.setEnabled(false);
                                filePositionalViewer.cleanAllMarkers();
                            }
                        } else {
                            // the value is correct
                            filePositionalViewer.setEnabled(true);
                            graphicRule.setEnabled(true);

                            // update the positionalViewer
                            filePositionalViewer.setSeparatorValue(value, filePositionalViewer.getVisible());
                            // update the field position Text
                            String newPosition = filePositionalViewer.calculatePositionX();
                            if (!fieldPositionText.getText().equals(newPosition)) {
                                fieldPositionText.setEditable(false);
                                fieldPositionText.setText(newPosition);
                            }
                            fieldPositionText.setEditable(true);
                        }
                        fieldSeparatorText.setSelection(selection.x);
                    }
                }
            }
        });

        // Separator Text : check Key Listener
        fieldSeparatorText.addKeyListener(new KeyAdapter() {

            @Override
            public void keyPressed(KeyEvent e) {
                e.doit = charIsAcceptedOnFieldSeparator(fieldSeparatorText.getText(), e.character, fieldSeparatorText
                        .getSelection().x);
            }
        });

        // Position Text : check the value and synchronise positionalViewer
        fieldPositionText.addModifyListener(new ModifyListener() {

            public void modifyText(final ModifyEvent e) {
                if (fieldPositionText.getEditable()) {
                    if (!checkFieldPositionValue()) {
                        // the value isn't correct => clean markers of the positionalViewer
                        fieldSeparatorText.setEditable(false);
                        if (filePositionalViewer.getVisible()) {
                            filePositionalViewer.setEnabled(false);
                            graphicRule.setEnabled(false);
                            filePositionalViewer.cleanAllMarkers();
                        }
                    } else {
                        filePositionalViewer.setEnabled(true);
                        graphicRule.setEnabled(true);
                        String value = getValidateFieldPosition(fieldPositionText.getText());
                        Point selection = fieldPositionText.getSelection();
                        // the value is correct
                        filePositionalViewer.setPositionValue(value, filePositionalViewer.getVisible());
                        fieldSeparatorText.setEditable(true);
                        value = filePositionalViewer.getSeparatorValue();
                        if (fieldSeparatorText.getText().equals("")) { //$NON-NLS-1$
                            fieldSeparatorText.setText("*"); //$NON-NLS-1$
                        } else if (fieldSeparatorText.getText().contains("*")) { //$NON-NLS-1$
                            fieldSeparatorText.setText(value + ",*"); //$NON-NLS-1$
                        } else {
                            fieldSeparatorText.setText(value);
                        }
                        fieldPositionText.setSelection(selection.x);
                    }
                }
            }
        });

        // Position Text : Key Listener
        fieldPositionText.addKeyListener(new KeyAdapter() {

            @Override
            public void keyPressed(KeyEvent e) {
                e.doit = charIsAcceptedOnFieldPosition(fieldPositionText.getText(), e.character,
                        fieldPositionText.getSelection().x);
            }
        });

    }

    /**
     * checkFileFieldsValue active fileViewer if file exist.
     */
    private void checkFilePathAndManageIt(boolean isNewFile) {

        filePathIsDone = false;
        if (fileField.getText() == "") { //$NON-NLS-1$
            filePositionalViewer.setText("\n" + Messages.getString("FileStep1.fileViewerTip1")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

        } else {
            filePositionalViewer.setText("\n" + Messages.getString("FileStep1.fileViewerProgress")); //$NON-NLS-1$ //$NON-NLS-2$

            StringBuffer previewRows = new StringBuffer(""); //$NON-NLS-1$
            BufferedReader in = null;

            try {

                File file = new File(fileField.getText());
                Charset guessedCharset = CharsetToolkit.guessEncoding(file, 4096);
                getConnection().setEncoding(guessedCharset.displayName());

                String str;
                int numberLine = 0;
                // read the file
                in = new BufferedReader(new InputStreamReader(new FileInputStream(fileField.getText()), guessedCharset
                        .displayName()));

                previewRows.append("\n");
                while (((str = in.readLine()) != null) && (numberLine <= maximumRowsToPreview)) {
                    numberLine++;
                    // replace Tabulation by a CaretChar
                    previewRows.append(str.replaceAll("\t", "\u25A1") + "\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                }

                // show lines
                filePositionalViewer.setText(new String(previewRows));
                filePathIsDone = true;
                if (isNewFile) {
                    fieldSeparatorText.setText("*"); //$NON-NLS-1$
                    filePositionalViewer.setSeparatorValue("*", true); //$NON-NLS-1$
                    getConnection().setFieldSeparatorValue("*"); //$NON-NLS-1$
                }

            } catch (Exception e) {
                String msgError = Messages.getString("FileStep1.filepath") + " \"" + fileField.getText().replace("\\\\", "\\") //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
                        + "\"\n"; //$NON-NLS-1$
                if (e instanceof FileNotFoundException) {
                    msgError = msgError + Messages.getString("FileStep1.fileNotFoundException"); //$NON-NLS-1$
                } else if (e instanceof EOFException) {
                    msgError = msgError + Messages.getString("FileStep1.eofException"); //$NON-NLS-1$
                } else if (e instanceof IOException) {
                    msgError = msgError + Messages.getString("FileStep1.fileLocked"); //$NON-NLS-1$
                } else {
                    msgError = Messages.getString("FileStep1.filepath") + " \"" + fileField.getText().replace("\\\\", "\\") //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
                            + "\" " + Messages.getString("FileStep1.noExist"); //$NON-NLS-1$ //$NON-NLS-2$
                }
                filePositionalViewer.setText("\n" + msgError); //$NON-NLS-1$
                updateStatus(IStatus.ERROR, msgError);
                log.error(msgError + " " + e.getMessage()); //$NON-NLS-1$
            } finally {
                String msgError = Messages.getString("FileStep1.filepath") + " \"" + fileField.getText().replace("\\\\", "\\") + "\"\n"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
                try {
                    if (in != null) {
                        in.close();
                    }
                } catch (IOException e) {
                    msgError = msgError + Messages.getString("FileStep1.fileLocked"); //$NON-NLS-1$
                }
            }

            // resize the composite text
            filePositionalViewer.layout();
            scrolledCompositeFileViewer.setMinSize(filePositionalViewer.computeSize(SWT.DEFAULT, SWT.DEFAULT));
        }

        checkFieldsValue();
    }

    /**
     * Ensures that fields are set.
     * 
     * @return
     */
    @Override
    protected boolean checkFieldsValue() {
        return checkFieldsValue(true);
    }

    /**
     * Ensures that fields are set.
     * 
     * @return
     */
    protected boolean checkFieldsValue(boolean checkViewerField) {
        // The fields
        // serverCombo.setEnabled(true);

        // if (serverCombo.getText() == "") { //$NON-NLS-1$
        // fileField.setEditable(false);
        // fileFormatCombo.setEnabled(false);
        // updateStatus(IStatus.ERROR, Messages.getString("FileStep1.serverAlert")); //$NON-NLS-1$
        // return false;
        // } else {
        fileField.setEditable(true);
        fileFormatCombo.setEnabled(true);
        // }

        if (fileField.getText() == "") { //$NON-NLS-1$
            updateStatus(IStatus.ERROR, Messages.getString("FileStep1.filepathAlert")); //$NON-NLS-1$
            return false;
        }

        if (!filePathIsDone) {
            updateStatus(IStatus.ERROR, Messages.getString("FileStep1.fileIncomplete")); //$NON-NLS-1$
            return false;
        } else if (fileFormatCombo.getSelectionIndex() < 0) {
            updateStatus(IStatus.ERROR, Messages.getString("FileStep1.formatAlert")); //$NON-NLS-1$
            return false;
        }

        updateStatus(IStatus.OK, null);
        if (checkViewerField) {
            if (checkFieldSeparatorValue()) {
                return checkFieldPositionValue();
            }
        }
        return true;
    }

    /**
     * Ensures that field Separator are set.
     * 
     * @return
     */
    protected boolean checkFieldSeparatorValue() {
        if (fieldSeparatorText.getText().length() <= 1) {
            // fieldSeparatorText can't be empty
            if (fieldSeparatorText.getText().equals("") || fieldSeparatorText.getText().equals("0")) { //$NON-NLS-1$ //$NON-NLS-2$
                updateStatus(IStatus.ERROR, Messages.getString("FileStep2.fieldSeparatorAlert")); //$NON-NLS-1$
                return false;
            } else if (fieldSeparatorText.getText().equals(",")) { //$NON-NLS-1$
                // fieldSeparatorText can't be just a comma
                updateStatus(IStatus.ERROR, Messages.getString("FileStep2.fieldSeparatorNotFinishByComma")); //$NON-NLS-1$
                return false;
            }
        } else if ((fieldSeparatorText.getText().substring(fieldSeparatorText.getText().length() - 1,
                fieldSeparatorText.getText().length()).equals(","))) { //$NON-NLS-1$
            // fieldSeparatorText can't finish by comma
            updateStatus(IStatus.ERROR, Messages.getString("FileStep2.fieldSeparatorNotFinishByComma")); //$NON-NLS-1$
            return false;
        } else if (!fieldSeparatorText.getText().equals(getValidateFieldSeparator(fieldSeparatorText.getText()))) {
            // fieldSeparatorText is not valide
            updateStatus(IStatus.ERROR, Messages.getString("FileStep2.fieldSeparatorAlert")); //$NON-NLS-1$
            return false;
        }

        checkFieldsValue(false);
        return true;
    }

    /**
     * Ensures that field Separator are set.
     * 
     * @return
     */
    protected boolean checkFieldPositionValue() {
        if (fieldPositionText.getText().length() <= 1) {
            // fieldPositionText can't be empty
            if (fieldPositionText.getText().equals("")) { //$NON-NLS-1$
                updateStatus(IStatus.ERROR, Messages.getString("FileStep2.fieldPositionAlert")); //$NON-NLS-1$
                return false;
            } else if (fieldPositionText.getText().equals(",")) { //$NON-NLS-1$
                // fieldPositionText can't be just a comma
                updateStatus(IStatus.ERROR, Messages.getString("FileStep2.fieldPositionNotFinishByComma")); //$NON-NLS-1$
                return false;
            }
        } else if (fieldPositionText.getText().substring(fieldPositionText.getText().length() - 1,
                fieldPositionText.getText().length()).equals(",")) { //$NON-NLS-1$
            // fieldPositionText can't finish by comma
            updateStatus(IStatus.ERROR, Messages.getString("FileStep2.fieldPositionNotFinishByComma")); //$NON-NLS-1$
            return false;
        }

        if (!fieldPositionText.getText().equals(getValidateFieldPosition(fieldPositionText.getText()))) {
            // fieldPositionText is not valide
            updateStatus(IStatus.ERROR, Messages.getString("FileStep2.fieldPositionNotValidate")); //$NON-NLS-1$
            return false;
        }

        checkFieldsValue(false);
        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.swt.widgets.Control#setVisible(boolean)
     * 
     */
    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);

        if (super.isVisible()) {
            // Adapt the UI fieldSeparator and Position to step1
            String value = getConnection().getFieldSeparatorValue();
            value = TalendTextUtils.removeQuotes(value);
            fieldSeparatorText.setText(value);
            if (value.endsWith("*")) {
                value = value.substring(0, value.length() - 1);
            }
            filePositionalViewer.setSeparatorValue(value, true);
            String newPosition = filePositionalViewer.calculatePositionX();
            if (!fieldPositionText.getText().equals(newPosition)) {
                fieldPositionText.setText(newPosition);
            }

            if (isReadOnly() != readOnly) {
                adaptFormToReadOnly();
            }
        }
    }

}
