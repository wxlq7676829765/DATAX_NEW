package com.alibaba.datax.common.element;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * Created by jingxing on 14-8-24.
 */

public interface Record {

	public void addColumn(Column column);

	public void setColumn(int i, final Column column);

	public Column getColumn(int i);

	public String toString();

	public int getColumnNumber();

	public int getByteSize();

	public int getMemorySize();

	List<Column> getColumnList();

	public void setFtpMap(Map map);

	public Map<String, Object> getFtpMap();

}
