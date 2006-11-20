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
package org.talend.designer.mapper.model.table;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.talend.core.model.metadata.IMetadataColumn;
import org.talend.core.model.metadata.IMetadataTable;
import org.talend.designer.mapper.external.data.ExternalMapperTable;
import org.talend.designer.mapper.external.data.ExternalMapperTableEntry;
import org.talend.designer.mapper.model.tableentry.AbstractInOutTableEntry;

/**
 * DOC amaumont class global comment. Detailled comment <br/>
 * 
 * $Id$
 * 
 */
public abstract class AbstractInOutTable extends AbstractDataMapTable {

    protected IMetadataTable metadataTable;

    /**
     * 
     * DOC amaumont AbstractInOutTable constructor comment.
     * 
     * @param metadataTable
     * @param externalMapperTable can be null
     * @param name
     */
    public AbstractInOutTable(IMetadataTable metadataTable, ExternalMapperTable externalMapperTable, String name) {
        super(name, externalMapperTable);
        this.metadataTable = metadataTable;
        initFromExternalData(externalMapperTable);
    }

    public AbstractInOutTable(IMetadataTable metadataTable, String name) {
        this(metadataTable, null, name);
    }

    private void initFromExternalData(ExternalMapperTable externalMapperTable) {
        List<IMetadataColumn> columns = this.metadataTable.getListColumns();
        Map<String, ExternalMapperTableEntry> nameToPerTabEntry = new HashMap<String, ExternalMapperTableEntry>();
        if (externalMapperTable != null && externalMapperTable.getMetadataTableEntries() != null) {
            for (ExternalMapperTableEntry perTableEntry : externalMapperTable.getMetadataTableEntries()) {
                nameToPerTabEntry.put(perTableEntry.getName(), perTableEntry);
            }
        }

        for (IMetadataColumn column : columns) {
            AbstractInOutTableEntry columnEntry = getNewTableEntry(column);
            ExternalMapperTableEntry externalMapperTableEntry = nameToPerTabEntry.get(columnEntry.getMetadataColumn().getLabel());
            if (externalMapperTableEntry != null) {
                columnEntry.setExpression(externalMapperTableEntry.getExpression());
            }
            dataMapTableEntries.add(columnEntry);
        }
    }

    protected abstract AbstractInOutTableEntry getNewTableEntry(IMetadataColumn metadataColumn);

    public IMetadataTable getMetadataTable() {
        return this.metadataTable;
    }

}
