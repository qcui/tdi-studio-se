// ============================================================================
//
// Talend Community Edition
//
// Copyright (C) 2006 Talend - www.talend.com
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
//
// ============================================================================
package org.talend.designer.mapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.exolab.castor.xml.MarshalException;
import org.exolab.castor.xml.Marshaller;
import org.exolab.castor.xml.Unmarshaller;
import org.exolab.castor.xml.ValidationException;
import org.talend.commons.exception.ExceptionHandler;
import org.talend.core.model.metadata.IMetadataTable;
import org.talend.core.model.process.AbstractExternalNode;
import org.talend.core.model.process.Problem;
import org.talend.core.model.temp.ECodePart;
import org.talend.designer.codegen.CodeGenerator;
import org.talend.designer.codegen.exception.CodeGeneratorException;
import org.talend.designer.mapper.external.data.ExternalMapperData;
import org.talend.designer.mapper.external.data.ExternalMapperTable;
import org.talend.designer.mapper.external.data.ExternalMapperTableEntry;
import org.talend.designer.mapper.language.LanguageProvider;
import org.talend.designer.mapper.model.tableentry.TableEntryLocation;
import org.talend.designer.mapper.utils.DataMapExpressionParser;
import org.talend.designer.mapper.utils.problems.ProblemsAnalyser;

/**
 * DOC amaumont class global comment. Detailled comment <br/>
 * 
 * $Id$
 * 
 */
public class MapperComponent extends AbstractExternalNode {

    private MapperMain mapperMain;

    private List<IMetadataTable> metadataListOut;

    private ExternalMapperData externalData;

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.core.model.process.AbstractExternalNode#initialize()
     */
    public void initialize() {
        initMapperMain();
        mapperMain.loadInitialParamters();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.designer.core.model.components.IExternalComponent#getPersistentData()
     */
    public Object getExternalData() {
        if (this.externalData == null) {
            this.externalData = new ExternalMapperData();
        }
        return this.externalData;
    }

    private void initMapperMain() {
        mapperMain = new MapperMain(this);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.designer.core.model.components.IExternalComponent#open(org.eclipse.swt.widgets.Display)
     */
    public int open(final Display display) {
        // TimeMeasure.start("Total open");
        // TimeMeasure.display = false;
        initMapperMain();
        mapperMain.loadFromExternalData(getIODataComponents(), getMetadataList(), externalData);
        Shell shell = mapperMain.createUI(display);
        // TimeMeasure.display = true;
        // TimeMeasure.end("Total open");
        while (!shell.isDisposed()) {
            try {
                if (!display.readAndDispatch()) {
                    display.sleep();
                }
            } catch (Throwable e) {
                if (MapperMain.isStandAloneMode()) {
                    e.printStackTrace();
                } else {
                    ExceptionHandler.process(e);
                }
            }
        }
        if (MapperMain.isStandAloneMode()) {
            display.dispose();
        }
        mapperMain.loadModelFromInternalData();
        refreshMapperConnectorData();
        return mapperMain.getMapperDialogResponse();
    }

    /**
     * DOC amaumont Comment method "refreshMapperConnectorData".
     */
    private void refreshMapperConnectorData() {
        metadataListOut = mapperMain.getMetadataListOut();
        externalData = mapperMain.buildExternalData();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.designer.core.model.components.IExternalComponent#open()
     */
    public int open(final Composite parent) {
        initMapperMain();
        mapperMain.loadFromExternalData(getIODataComponents(), getMetadataList(), externalData);
        mapperMain.createUI(parent);
        return mapperMain.getMapperDialogResponse();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.designer.core.model.components.IExternalComponent#setPersistentData(java.lang.Object)
     */
    public void setExternalData(Object externalData) {
        this.externalData = (ExternalMapperData) externalData;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.core.model.process.INode#getGeneratedCode()
     */
    public String getGeneratedCode() {
        try {
            return new CodeGenerator().generateComponentCode(this, ECodePart.MAIN);
        } catch (CodeGeneratorException e) {
            ExceptionHandler.process(e);
        }
        return "";
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.core.model.process.INode#getMetadataList()
     */
    public List<IMetadataTable> getMetadataList() {
        return this.metadataListOut;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.core.model.process.INode#setMetadataList(java.util.List)
     */
    public void setMetadataList(List<IMetadataTable> metadataTablesOut) {
        this.metadataListOut = metadataTablesOut;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.core.model.process.AbstractExternalNode#setExternalXmlData(java.io.InputStream)
     */
    public void loadDataIn(InputStream in, Reader stringReader) throws IOException, ClassNotFoundException {
        // if (in.available() > 0) {
        // ObjectInputStream oin = null;
        // try {
        // oin = new ObjectInputStream(in);
        // externalData = (ExternalMapperData) oin.readObject();
        //
        // } finally {
        // if (oin != null) {
        // oin.close();
        // }
        // }
        // }

        if (stringReader != null) {
            Unmarshaller unmarshaller = new Unmarshaller(ExternalMapperData.class);
            try {
                externalData = (ExternalMapperData) unmarshaller.unmarshal(stringReader);
            } catch (MarshalException e) {
                ExceptionHandler.process(e);
            } catch (ValidationException e) {
                ExceptionHandler.process(e);
            } finally {
                if (stringReader != null) {
                    stringReader.close();
                }
            }
        }

    }

    public void loadDataOut(final OutputStream out, Writer writer) throws IOException {

        initMapperMain();
        mapperMain.loadFromExternalData(getIncomingConnections(), getOutgoingConnections(), externalData, getMetadataList());
        ExternalMapperData data = mapperMain.buildExternalData();
        if (mapperMain != null && data != null) {

            try {
                Marshaller marshaller = new Marshaller(writer);
                marshaller.marshal(externalData);
            } catch (MarshalException e) {
                ExceptionHandler.process(e);
            } catch (ValidationException e) {
                ExceptionHandler.process(e);
            } catch (IOException e) {
                ExceptionHandler.process(e);
            } finally {
                if (writer != null) {
                    writer.close();
                }
            }

            // ObjectOutputStream objectOut = null;
            // try {
            // objectOut = new ObjectOutputStream(out);
            // objectOut.writeObject(data);
            // } catch (IOException e) {
            // ExceptionHandler.process(e);
            // } finally {
            // if (objectOut != null) {
            // objectOut.close();
            // }
            // }
        }
    }

    public void renameInputConnection(String oldConnectionName, String newConnectionName) {
        if (oldConnectionName == null || newConnectionName == null) {
            throw new NullPointerException();
        }
        if (externalData != null) {
            List<ExternalMapperTable> inputTables = externalData.getInputTables();
            for (ExternalMapperTable table : inputTables) {
                if (table.getName().equals(oldConnectionName)) {
                    table.setName(newConnectionName);
                    TableEntryLocation oldLocation = new TableEntryLocation(oldConnectionName, null);
                    TableEntryLocation newLocation = new TableEntryLocation(newConnectionName, null);
                    replaceLocationsInAllExpressions(oldLocation, newLocation, true);
                    break;
                }
            }
        }
    }

    public void renameOutputConnection(String oldName, String newName) {
        if (oldName == null || newName == null) {
            throw new NullPointerException();
        }
        if (externalData != null) {
            List<ExternalMapperTable> outputTables = externalData.getOutputTables();
            for (ExternalMapperTable table : outputTables) {
                if (table.getName().equals(oldName)) {
                    table.setName(newName);
                    break;
                }
            }
        }
    }

    protected void renameMetadataColumnName(String conectionName, String oldColumnName, String newColumnName) {
        if (conectionName == null || oldColumnName == null || newColumnName == null) {
            throw new NullPointerException();
        }
        if (externalData != null) {
            // rename metadata column name
            List<ExternalMapperTable> tables = new ArrayList<ExternalMapperTable>(externalData.getInputTables());
            tables.addAll(externalData.getOutputTables());
            ExternalMapperTable tableFound = null;
            for (ExternalMapperTable table : tables) {
                if (table.getName().equals(conectionName)) {
                    List<ExternalMapperTableEntry> metadataTableEntries = table.getMetadataTableEntries();
                    for (ExternalMapperTableEntry entry : metadataTableEntries) {
                        if (entry.getName().equals(oldColumnName)) {
                            entry.setName(newColumnName);
                            tableFound = table;
                            break;
                        }
                    }
                    break;
                }
            }

            // it is necessary to update expressions only if renamed column come from input table
            if (tableFound != null && externalData.getInputTables().indexOf(tableFound) != -1) {
                TableEntryLocation oldLocation = new TableEntryLocation(conectionName, oldColumnName);
                TableEntryLocation newLocation = new TableEntryLocation(conectionName, newColumnName);
                replaceLocationsInAllExpressions(oldLocation, newLocation, false);
            }

        }
    }

    /**
     * DOC amaumont Comment method "replaceLocations".
     * 
     * @param oldLocation
     * @param newLocation
     * @param tableRenamed TODO
     * @param newTableName
     * @param newColumnName
     */
    private void replaceLocationsInAllExpressions(TableEntryLocation oldLocation, TableEntryLocation newLocation, boolean tableRenamed) {
        // replace old location by new location for all expressions in mapper
        List<ExternalMapperTable> tables = new ArrayList<ExternalMapperTable>(externalData.getInputTables());
        tables.addAll(new ArrayList<ExternalMapperTable>(externalData.getVarsTables()));
        tables.addAll(new ArrayList<ExternalMapperTable>(externalData.getOutputTables()));
        DataMapExpressionParser dataMapExpressionParser = new DataMapExpressionParser(LanguageProvider.getCurrentLanguage());
        // loop on all tables
        for (ExternalMapperTable table : tables) {
            List<ExternalMapperTableEntry> metadataTableEntries = table.getMetadataTableEntries();
            if (metadataTableEntries != null) {
                // loop on all entries of current table
                for (ExternalMapperTableEntry entry : metadataTableEntries) {
                    replaceLocation(oldLocation, newLocation, entry, dataMapExpressionParser, tableRenamed);
                } // for (ExternalMapperTableEntry entry : metadataTableEntries) {
            }
            if (table.getConstraintTableEntries() != null) {
                for (ExternalMapperTableEntry entry : table.getConstraintTableEntries()) {
                    replaceLocation(oldLocation, newLocation, entry, dataMapExpressionParser, tableRenamed);
                }
            }
        } // for (ExternalMapperTable table : tables) {
    }

    public void replaceLocation(TableEntryLocation oldLocation, TableEntryLocation newLocation, ExternalMapperTableEntry entry,
            DataMapExpressionParser dataMapExpressionParser, boolean tableRenamed) {
        String currentExpression = entry.getExpression();
        if (currentExpression == null || currentExpression.length() == 0) {
            return;
        }
        TableEntryLocation[] tableEntryLocations = dataMapExpressionParser.parseTableEntryLocations(currentExpression);
        // loop on all locations of current expression
        for (int i = 0; i < tableEntryLocations.length; i++) {
            TableEntryLocation currentLocation = tableEntryLocations[i];
            if (tableRenamed && oldLocation.tableName.equals(currentLocation.tableName)) {
                oldLocation.columnName = currentLocation.columnName;
                newLocation.columnName = currentLocation.columnName;
            }
            if (currentLocation.equals(oldLocation)) {
                currentExpression = dataMapExpressionParser.replaceLocation(currentExpression, currentLocation, newLocation);
            }
        } // for (int i = 0; i < tableEntryLocations.length; i++) {
        entry.setExpression(currentExpression);

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.core.model.process.AbstractExternalNode#getProblems()
     */
    @Override
    public List<Problem> getProblems() {
        initMapperMain();
        ProblemsAnalyser problemsAnalyser = new ProblemsAnalyser(mapperMain.getMapperManager());
        return problemsAnalyser.checkProblems(externalData);
    }

}
