package com.alibaba.datax.plugin.unstructuredstorage.reader;

import com.alibaba.datax.common.element.*;
import com.alibaba.datax.common.exception.DataXException;
import com.alibaba.datax.common.plugin.RecordSender;
import com.alibaba.datax.common.plugin.TaskPluginCollector;
import com.alibaba.datax.common.util.Configuration;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.csvreader.CsvReader;
import io.airlift.compress.snappy.SnappyCodec;
import io.airlift.compress.snappy.SnappyFramedInputStream;
import org.anarres.lzo.LzoDecompressor1x_safe;
import org.anarres.lzo.LzoInputStream;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.hadoop.io.compress.CompressionCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.UnsupportedCharsetException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class UnstructuredStorageReaderUtil {
	private static final Logger LOG = LoggerFactory
			.getLogger(UnstructuredStorageReaderUtil.class);
	public static HashMap<String, Object> csvReaderConfigMap;

	private UnstructuredStorageReaderUtil() {

	}

	/**
	 * @param inputLine
	 *            输入待分隔字符串
	 * @param delimiter
	 *            字符串分割符
	 * @return 分隔符分隔后的字符串数组，出现异常时返回为null 支持转义，即数据中可包含分隔符
	 * */
	public static String[] splitOneLine(String inputLine, char delimiter) {
		String[] splitedResult = null;
		if (null != inputLine) {
			try {
				CsvReader csvReader = new CsvReader(new StringReader(inputLine));
				csvReader.setDelimiter(delimiter);

				setCsvReaderConfig(csvReader);

				if (csvReader.readRecord()) {
					splitedResult = csvReader.getValues();
				}
			} catch (IOException e) {
				// nothing to do
			}
		}
		return splitedResult;
	}

	public static String[] splitBufferedReader(CsvReader csvReader)
			throws IOException {
		String[] splitedResult = null;
		if (csvReader.readRecord()) {
			splitedResult = csvReader.getValues();
		}
		return splitedResult;
	}

	/**
	 * 不支持转义
	 *
	 * @return 分隔符分隔后的字符串数，
	 * */
	public static String[] splitOneLine(String inputLine, String delimiter) {
		String[] splitedResult = StringUtils.split(inputLine, delimiter);
		return splitedResult;
	}

	/**
	 * @param inputStream 文件流
	 * @param context 路径 /sourceCsv/source1.csv
	 * @param readerSliceConfig read 的 基本json相关信息
	 * @param recordSender 数据记录 record
	 * @param taskPluginCollector 这个task 的josn 信息
	 */
	public static void readFromStream(InputStream inputStream, String context,
									  Configuration readerSliceConfig, RecordSender recordSender,
									  TaskPluginCollector taskPluginCollector) throws IOException {


		String compress = readerSliceConfig.getString(Key.COMPRESS, null);
		if (StringUtils.isBlank(compress)) {
			compress = null;
		}
		String encoding = readerSliceConfig.getString(Key.ENCODING,
				Constant.DEFAULT_ENCODING);
		// handle blank encoding
		if (StringUtils.isBlank(encoding)) {
			encoding = Constant.DEFAULT_ENCODING;
			LOG.warn(String.format("您配置的encoding为[%s], 使用默认值[%s]", encoding,
					Constant.DEFAULT_ENCODING));
		}

		List<Configuration> column = readerSliceConfig
				.getListConfiguration(Key.COLUMN);
		// handle ["*"] -> [], null
		if (null != column && 1 == column.size()
				&& "\"*\"".equals(column.get(0).toString())) {
			readerSliceConfig.set(Key.COLUMN, null);
			column = null;
		}

		BufferedReader reader = null;
		int bufferSize = readerSliceConfig.getInt(Key.BUFFER_SIZE,
				Constant.DEFAULT_BUFFER_SIZE);

		// compress logic
		try {
			if (null == compress) {
				reader = new BufferedReader(new InputStreamReader(inputStream,
						encoding), bufferSize);
			} else {
				// TODO compress
				if ("lzo_deflate".equalsIgnoreCase(compress)) {
					LzoInputStream lzoInputStream = new LzoInputStream(
							inputStream, new LzoDecompressor1x_safe());
					reader = new BufferedReader(new InputStreamReader(
							lzoInputStream, encoding));
				} else if ("lzo".equalsIgnoreCase(compress)) {
					LzoInputStream lzopInputStream = new ExpandLzopInputStream(
							inputStream);
					reader = new BufferedReader(new InputStreamReader(
							lzopInputStream, encoding));
				} else if ("gzip".equalsIgnoreCase(compress)) {
					CompressorInputStream compressorInputStream = new GzipCompressorInputStream(
							inputStream);
					reader = new BufferedReader(new InputStreamReader(
							compressorInputStream, encoding), bufferSize);
				} else if ("bzip2".equalsIgnoreCase(compress)) {
					CompressorInputStream compressorInputStream = new BZip2CompressorInputStream(
							inputStream);
					reader = new BufferedReader(new InputStreamReader(
							compressorInputStream, encoding), bufferSize);
				} else if ("hadoop-snappy".equalsIgnoreCase(compress)) {
					CompressionCodec snappyCodec = new SnappyCodec();
					InputStream snappyInputStream = snappyCodec.createInputStream(
							inputStream);
					reader = new BufferedReader(new InputStreamReader(
							snappyInputStream, encoding));
				} else if ("framing-snappy".equalsIgnoreCase(compress)) {
					InputStream snappyInputStream = new SnappyFramedInputStream(
							inputStream);
					reader = new BufferedReader(new InputStreamReader(
							snappyInputStream, encoding));
				}/* else if ("lzma".equalsIgnoreCase(compress)) {
					CompressorInputStream compressorInputStream = new LZMACompressorInputStream(
							inputStream);
					reader = new BufferedReader(new InputStreamReader(
							compressorInputStream, encoding));
				} *//*else if ("pack200".equalsIgnoreCase(compress)) {
					CompressorInputStream compressorInputStream = new Pack200CompressorInputStream(
							inputStream);
					reader = new BufferedReader(new InputStreamReader(
							compressorInputStream, encoding));
				} *//*else if ("xz".equalsIgnoreCase(compress)) {
					CompressorInputStream compressorInputStream = new XZCompressorInputStream(
							inputStream);
					reader = new BufferedReader(new InputStreamReader(
							compressorInputStream, encoding));
				} else if ("ar".equalsIgnoreCase(compress)) {
					ArArchiveInputStream arArchiveInputStream = new ArArchiveInputStream(
							inputStream);
					reader = new BufferedReader(new InputStreamReader(
							arArchiveInputStream, encoding));
				} else if ("arj".equalsIgnoreCase(compress)) {
					ArjArchiveInputStream arjArchiveInputStream = new ArjArchiveInputStream(
							inputStream);
					reader = new BufferedReader(new InputStreamReader(
							arjArchiveInputStream, encoding));
				} else if ("cpio".equalsIgnoreCase(compress)) {
					CpioArchiveInputStream cpioArchiveInputStream = new CpioArchiveInputStream(
							inputStream);
					reader = new BufferedReader(new InputStreamReader(
							cpioArchiveInputStream, encoding));
				} else if ("dump".equalsIgnoreCase(compress)) {
					DumpArchiveInputStream dumpArchiveInputStream = new DumpArchiveInputStream(
							inputStream);
					reader = new BufferedReader(new InputStreamReader(
							dumpArchiveInputStream, encoding));
				} else if ("jar".equalsIgnoreCase(compress)) {
					JarArchiveInputStream jarArchiveInputStream = new JarArchiveInputStream(
							inputStream);
					reader = new BufferedReader(new InputStreamReader(
							jarArchiveInputStream, encoding));
				} else if ("tar".equalsIgnoreCase(compress)) {
					TarArchiveInputStream tarArchiveInputStream = new TarArchiveInputStream(
							inputStream);
					reader = new BufferedReader(new InputStreamReader(
							tarArchiveInputStream, encoding));
				}*/
				else if ("zip".equalsIgnoreCase(compress)) {
					ZipCycleInputStream zipCycleInputStream = new ZipCycleInputStream(
							inputStream);
					reader = new BufferedReader(new InputStreamReader(
							zipCycleInputStream, encoding), bufferSize);
				} else {
					throw DataXException
							.asDataXException(
									UnstructuredStorageReaderErrorCode.ILLEGAL_VALUE,
									String.format("仅支持 gzip, bzip2, zip, lzo, lzo_deflate, hadoop-snappy, framing-snappy" +
											"文件压缩格式 , 不支持您配置的文件压缩格式: [%s]", compress));
				}
			}
			//文件转 字节集
			String fileFormat = readerSliceConfig.getString(Key.FILE_FORMAT);
			if("file2file".equals(fileFormat)||"file2file".equals(fileFormat)){
				byte[] buffer = IOUtils.toByteArray(inputStream);
				UnstructuredStorageReaderUtil.doReadFromStream(inputStream,reader, context,
						readerSliceConfig, recordSender, taskPluginCollector,buffer);
			}else{
				byte[] buffer = new byte[1024];
						UnstructuredStorageReaderUtil.doReadFromStream(inputStream,reader, context,
						readerSliceConfig, recordSender, taskPluginCollector,buffer);
			}


		} catch (UnsupportedEncodingException uee) {
			throw DataXException
					.asDataXException(
							UnstructuredStorageReaderErrorCode.OPEN_FILE_WITH_CHARSET_ERROR,
							String.format("不支持的编码格式 : [%s]", encoding), uee);
		} catch (NullPointerException e) {
			throw DataXException.asDataXException(
					UnstructuredStorageReaderErrorCode.RUNTIME_EXCEPTION,
					"运行时错误, 请联系我们", e);
		}/* catch (ArchiveException e) {
			throw DataXException.asDataXException(
					UnstructuredStorageReaderErrorCode.READ_FILE_IO_ERROR,
					String.format("压缩文件流读取错误 : [%s]", context), e);
		} */catch (IOException e) {
			throw DataXException.asDataXException(
					UnstructuredStorageReaderErrorCode.READ_FILE_IO_ERROR,
					String.format("流读取错误 : [%s]", context), e);
		} finally {
			inputStream.close();
			IOUtils.closeQuietly(reader);
		}

	}




	public static byte[] file2Byte(InputStream inputStream)  {

		try{
			/*文件流写成字符集*/
			int size = inputStream.available();
			byte[] buffer = new byte[size];
			inputStream.read(buffer);

			//2、第二步：byte数组转换为char数组
			char[] byToChs = new char[size];
			for(int i =0;i<size;i++){
				byToChs[i] = (char) buffer[i];
			}

			//3、第三步：char数组转换为字符串
			StringBuilder sb = new StringBuilder();
			sb.append(byToChs);
			String str = sb.toString();

			//4、第四步：字符串转换为char数组
			char[] strTochs = str.toCharArray();

			//5、第五步:char数组转换为byte数组
			for(int i = 0;i<size;i++){
				buffer[i] = (byte) strTochs[i];
			}
			return buffer;
		}catch (IOException e){
			return null;
		}
	}

	public static void readFromJSONArray(JSONArray jsonArray,
										 Configuration readerSliceConfig, RecordSender recordSender,
										 TaskPluginCollector taskPluginCollector) {
		// column: 1. index type 2.value type 3.when type is Data, may have format
		List<ColumnEntry> column = UnstructuredStorageReaderUtil
				.getListColumnEntry(readerSliceConfig, com.alibaba.datax.plugin.unstructuredstorage.reader.Key.COLUMN);

		for (Object o : jsonArray) {
			Map map = (Map) o;
			String[] sourceLine = new String[map.size()];
			int i = 0;
			for (Object entry : map.entrySet()){
//                    System.out.println(((Map.Entry)entry).getKey()  + " : " +((Map.Entry)entry).getValue());
				sourceLine[i]=((Map.Entry)entry).getValue().toString();
				i++;
			}
			transportOneRecord2(recordSender,column,sourceLine,taskPluginCollector);
		}
	}

	public static void doReadFromStream(InputStream inputStream,BufferedReader reader, String context,
										Configuration readerSliceConfig, RecordSender recordSender,
										TaskPluginCollector taskPluginCollector,byte[] buffer) throws IOException {
		String encoding = readerSliceConfig.getString(Key.ENCODING,
				Constant.DEFAULT_ENCODING);
		Character fieldDelimiter = null;
		/*分割符号“,”*/
		String delimiterInStr = readerSliceConfig
				.getString(Key.FIELD_DELIMITER);
		if (null != delimiterInStr && 1 != delimiterInStr.length()) {
			throw DataXException.asDataXException(
					UnstructuredStorageReaderErrorCode.ILLEGAL_VALUE,
					String.format("仅仅支持单字符切分, 您配置的切分为 : [%s]", delimiterInStr));
		}
		if (null == delimiterInStr) {
			LOG.warn(String.format("您没有配置列分隔符, 使用默认值[%s]",
					Constant.DEFAULT_FIELD_DELIMITER));
		}

		// warn: default value ',', fieldDelimiter could be \n(lineDelimiter)
		// for no fieldDelimiter
		fieldDelimiter = readerSliceConfig.getChar(Key.FIELD_DELIMITER,
				Constant.DEFAULT_FIELD_DELIMITER);
		Boolean skipHeader = readerSliceConfig.getBool(Key.SKIP_HEADER,
				Constant.DEFAULT_SKIP_HEADER);
		// warn: no default value '\N'
		String nullFormat = readerSliceConfig.getString(Key.NULL_FORMAT);

		String fileFormat = readerSliceConfig.getString(Key.FILE_FORMAT);
		// warn: Configuration -> List<ColumnEntry> for performance
		// List<Configuration> column = readerSliceConfig
		// .getListConfiguration(Key.COLUMN);
		List<ColumnEntry> column = UnstructuredStorageReaderUtil
				.getListColumnEntry(readerSliceConfig, Key.COLUMN);
		CsvReader csvReader  = null;

		// every line logic
		try {
			// TODO lineDelimiter
			if (skipHeader) {
				String fetchLine = reader.readLine();
				LOG.info(String.format("Header line %s has been skiped.",
						fetchLine));
			}
			csvReader = new CsvReader(reader);
			csvReader.setDelimiter(fieldDelimiter);
			Object[] parseRows;
			if("file2file".equals(fileFormat)){
				UnstructuredStorageReaderUtil.transportOneRecordFtp(recordSender,new ArrayList<ColumnEntry>(), new String[0], nullFormat, taskPluginCollector,context,buffer);
			}else if("file2DB".equals(fileFormat)){
				Object[] file2DB = new Object[4];
				JSONObject ob = fileNameFinal(context);
				file2DB[0] = ob.get("fileName");
				file2DB[1] = ob.get("sourceFilePath");
				file2DB[2] = Base64.getEncoder().encodeToString(buffer);
				file2DB[3] = getTimeNow();
				parseRows = file2DB;
				if(buffer!=null){
					UnstructuredStorageReaderUtil.transportOneRecordFtp(recordSender,column, parseRows, nullFormat, taskPluginCollector,context,buffer);
				}
			}else{
				String[] parseRows1;
				setCsvReaderConfig(csvReader);
				while ((parseRows1 = UnstructuredStorageReaderUtil.splitBufferedReader(csvReader)) != null) {
					/*20230323 wx 修改原本的文件读取，添加inputStream类型，直接传输*/
					//UnstructuredStorageReaderUtil.transportOneRecord(recordSender,column, parseRows, nullFormat, taskPluginCollector);
					UnstructuredStorageReaderUtil.transportOneRecordFtp(recordSender,column, parseRows1, nullFormat, taskPluginCollector,context,buffer);
				}
			}

		} catch (UnsupportedEncodingException uee) {
			throw DataXException
					.asDataXException(
							UnstructuredStorageReaderErrorCode.OPEN_FILE_WITH_CHARSET_ERROR,
							String.format("不支持的编码格式 : [%s]", encoding), uee);
		} catch (FileNotFoundException fnfe) {
			throw DataXException.asDataXException(
					UnstructuredStorageReaderErrorCode.FILE_NOT_EXISTS,
					String.format("无法找到文件 : [%s]", context), fnfe);
		} catch (IOException ioe) {
			throw DataXException.asDataXException(
					UnstructuredStorageReaderErrorCode.READ_FILE_IO_ERROR,
					String.format("读取文件错误 : [%s]", context), ioe);
		} catch (Exception e) {
			throw DataXException.asDataXException(
					UnstructuredStorageReaderErrorCode.RUNTIME_EXCEPTION,
					String.format("运行时异常 : %s", e.getMessage()), e);
		} finally {
			inputStream.close();
			csvReader.close();
			IOUtils.closeQuietly(reader);
		}
	}

	public static Record transportOneRecord(RecordSender recordSender,
											Configuration configuration,
											TaskPluginCollector taskPluginCollector,
											String line){
		List<ColumnEntry> column = UnstructuredStorageReaderUtil
				.getListColumnEntry(configuration, Key.COLUMN);
		// 注意: nullFormat 没有默认值
		String nullFormat = configuration.getString(Key.NULL_FORMAT);
		String delimiterInStr = configuration.getString(Key.FIELD_DELIMITER);
		if (null != delimiterInStr && 1 != delimiterInStr.length()) {
			throw DataXException.asDataXException(
					UnstructuredStorageReaderErrorCode.ILLEGAL_VALUE,
					String.format("仅仅支持单字符切分, 您配置的切分为 : [%s]", delimiterInStr));
		}
		if (null == delimiterInStr) {
			LOG.warn(String.format("您没有配置列分隔符, 使用默认值[%s]",
					Constant.DEFAULT_FIELD_DELIMITER));
		}
		// warn: default value ',', fieldDelimiter could be \n(lineDelimiter)
		// for no fieldDelimiter
		Character fieldDelimiter = configuration.getChar(Key.FIELD_DELIMITER,
				Constant.DEFAULT_FIELD_DELIMITER);

		String[] sourceLine = StringUtils.split(line, fieldDelimiter);

		return transportOneRecord(recordSender, column, sourceLine, nullFormat, taskPluginCollector);
	}


	/**
	 * 2023/02/23 wx 修改原本的文件读取，添加inputStream类型，直接传输
	 * @param recordSender record
	 * @param columnConfigs
	 * @param sourceLine
	 * @param nullFormat
	 * @param taskPluginCollector
	 * @return
	 */
	public static Record transportOneRecordFtp(RecordSender recordSender,
											List<ColumnEntry> columnConfigs, Object[] sourceLine,
											String nullFormat, TaskPluginCollector taskPluginCollector,String filePath,byte[] buffer) {
		Record record = recordSender.createRecord();
		Column columnGenerated = null;
		// 创建都为String类型column的record
		if (null == columnConfigs || columnConfigs.size() == 0) {
			for (Object columnValue : sourceLine) {
				// not equalsIgnoreCase, it's all ok if nullFormat is null
				if (columnValue.equals(nullFormat)) {
					columnGenerated = new StringColumn(null);
				} else {
					columnGenerated = new StringColumn(String.valueOf(columnValue));
				}
				record.addColumn(columnGenerated);
			}
			/*20230224 wx 写入*/
			record.setFtpMap(getMapByFileNameAndByte(filePath,buffer));
			recordSender.sendToWriter(record);
		} else {
			try {
				for (ColumnEntry columnConfig : columnConfigs) {
					String columnType = columnConfig.getType();
					Integer columnIndex = columnConfig.getIndex();
					String columnConst = columnConfig.getValue();

					Object columnValue = null;

					if (null == columnIndex && null == columnConst) {
						throw DataXException
								.asDataXException(
										UnstructuredStorageReaderErrorCode.NO_INDEX_VALUE,
										"由于您配置了type, 则至少需要配置 index 或 value");
					}

					if (null != columnIndex && null != columnConst) {
						throw DataXException
								.asDataXException(
										UnstructuredStorageReaderErrorCode.MIXED_INDEX_VALUE,
										"您混合配置了index, value, 每一列同时仅能选择其中一种");
					}

					if (null != columnIndex) {
						if (columnIndex >= sourceLine.length) {
							String message = String
									.format("您尝试读取的列越界,源文件该行有 [%s] 列,您尝试读取第 [%s] 列, 数据详情[%s]",
											sourceLine.length, columnIndex + 1,
											StringUtils.join(sourceLine, ","));
							LOG.warn(message);
							throw new IndexOutOfBoundsException(message);
						}

						columnValue = sourceLine[columnIndex];
					} else {
						columnValue = columnConst;
					}
					Type type = Type.valueOf(columnType.toUpperCase());
					// it's all ok if nullFormat is null
					if (columnValue.equals(nullFormat)) {columnValue = null;
					}
					switch (type) {
						case STRING:
							columnGenerated = new StringColumn(String.valueOf(columnValue));
							break;
						case LONG:
							try {
								columnGenerated = new LongColumn(String.valueOf(columnValue));
							} catch (Exception e) {
								throw new IllegalArgumentException(String.format(
										"类型转换错误, 无法将[%s] 转换为[%s]", columnValue,
										"LONG"));
							}
							break;
						case DOUBLE:
							try {
								columnGenerated = new DoubleColumn(String.valueOf(columnValue));
							} catch (Exception e) {
								throw new IllegalArgumentException(String.format(
										"类型转换错误, 无法将[%s] 转换为[%s]", columnValue,
										"DOUBLE"));
							}
							break;
						case BYTE:
							try {
								columnGenerated = new BytesColumn((byte[]) columnValue);
							} catch (Exception e) {
								throw new IllegalArgumentException(String.format(
										"类型转换错误, 无法将[%s] 转换为[%s]", columnValue,
										"BYTE"));
							}
							break;
						case BOOLEAN:
							try {
								columnGenerated = new BoolColumn(String.valueOf(columnValue));
							} catch (Exception e) {
							}
							throw new IllegalArgumentException(String.format(
										"类型转换错误, 无法将[%s] 转换为[%s]", columnValue,
										"BOOLEAN"));
						case DATE:
							try {
								if (columnValue == null) {
									Date date = null;
									columnGenerated = new DateColumn(date);
								} else {
									String formatString = columnConfig.getFormat();
									//if (null != formatString) {
									if (StringUtils.isNotBlank(formatString)) {
										// 用户自己配置的格式转换, 脏数据行为出现变化
										DateFormat format = columnConfig
												.getDateFormat();
										columnGenerated = new DateColumn(
												format.parse(String.valueOf(columnValue)));
									} else {
										// 框架尝试转换
										columnGenerated = new DateColumn(
												new StringColumn(String.valueOf(columnValue))
														.asDate());
									}
								}
							} catch (Exception e) {
								throw new IllegalArgumentException(String.format(
										"类型转换错误, 无法将[%s] 转换为[%s]", columnValue,
										"DATE"));
							}
							break;
						default:
							String errorMessage = String.format(
									"您配置的列类型暂不支持 : [%s]", columnType);
							LOG.error(errorMessage);
							throw DataXException
									.asDataXException(
											UnstructuredStorageReaderErrorCode.NOT_SUPPORT_TYPE,
											errorMessage);
					}

					record.addColumn(columnGenerated);

				}
				/*20230224 wx 存入io数据流*/
				record.setFtpMap(getMapByFileNameAndByte(filePath,buffer));
				recordSender.sendToWriter(record);
			} catch (IllegalArgumentException iae) {
				taskPluginCollector
						.collectDirtyRecord(record, iae.getMessage());
			} catch (IndexOutOfBoundsException ioe) {
				taskPluginCollector
						.collectDirtyRecord(record, ioe.getMessage());
			} catch (Exception e) {
				if (e instanceof DataXException) {
					throw (DataXException) e;
				}
				// 每一种转换失败都是脏数据处理,包括数字格式 & 日期格式
				taskPluginCollector.collectDirtyRecord(record, e.getMessage());
			}
		}

		return record;
	}

	public static Record transportOneRecord(RecordSender recordSender,
											List<ColumnEntry> columnConfigs, String[] sourceLine,
											String nullFormat, TaskPluginCollector taskPluginCollector) {
		Record record = recordSender.createRecord();
		Column columnGenerated = null;

		// 创建都为String类型column的record
		if (null == columnConfigs || columnConfigs.size() == 0) {
			for (String columnValue : sourceLine) {
				// not equalsIgnoreCase, it's all ok if nullFormat is null
				if (columnValue.equals(nullFormat)) {
					columnGenerated = new StringColumn(null);
				} else {
					columnGenerated = new StringColumn(columnValue);
				}
				record.addColumn(columnGenerated);
			}
			recordSender.sendToWriter(record);
		} else {
			try {
				for (ColumnEntry columnConfig : columnConfigs) {
					String columnType = columnConfig.getType();
					Integer columnIndex = columnConfig.getIndex();
					String columnConst = columnConfig.getValue();

					String columnValue = null;

					if (null == columnIndex && null == columnConst) {
						throw DataXException
								.asDataXException(
										UnstructuredStorageReaderErrorCode.NO_INDEX_VALUE,
										"由于您配置了type, 则至少需要配置 index 或 value");
					}

					if (null != columnIndex && null != columnConst) {
						throw DataXException
								.asDataXException(
										UnstructuredStorageReaderErrorCode.MIXED_INDEX_VALUE,
										"您混合配置了index, value, 每一列同时仅能选择其中一种");
					}

					if (null != columnIndex) {
						if (columnIndex >= sourceLine.length) {
							String message = String
									.format("您尝试读取的列越界,源文件该行有 [%s] 列,您尝试读取第 [%s] 列, 数据详情[%s]",
											sourceLine.length, columnIndex + 1,
											StringUtils.join(sourceLine, ","));
							LOG.warn(message);
							throw new IndexOutOfBoundsException(message);
						}

						columnValue = sourceLine[columnIndex];
					} else {
						columnValue = columnConst;
					}
					Type type = Type.valueOf(columnType.toUpperCase());
					// it's all ok if nullFormat is null
					if (columnValue.equals(nullFormat)) {columnValue = null;
					}
					switch (type) {
						case STRING:
							columnGenerated = new StringColumn(columnValue);
							break;
						case LONG:
							try {
								columnGenerated = new LongColumn(columnValue);
							} catch (Exception e) {
								throw new IllegalArgumentException(String.format(
										"类型转换错误, 无法将[%s] 转换为[%s]", columnValue,
										"LONG"));
							}
							break;
						case DOUBLE:
							try {
								columnGenerated = new DoubleColumn(columnValue);
							} catch (Exception e) {
								throw new IllegalArgumentException(String.format(
										"类型转换错误, 无法将[%s] 转换为[%s]", columnValue,
										"DOUBLE"));
							}
							break;
						case BOOLEAN:
							try {
								columnGenerated = new BoolColumn(columnValue);
							} catch (Exception e) {
								throw new IllegalArgumentException(String.format(
										"类型转换错误, 无法将[%s] 转换为[%s]", columnValue,
										"BOOLEAN"));
							}

							break;
						case DATE:
							try {
								if (columnValue == null) {
									Date date = null;
									columnGenerated = new DateColumn(date);
								} else {
									String formatString = columnConfig.getFormat();
									//if (null != formatString) {
									if (StringUtils.isNotBlank(formatString)) {
										// 用户自己配置的格式转换, 脏数据行为出现变化
										DateFormat format = columnConfig
												.getDateFormat();
										columnGenerated = new DateColumn(
												format.parse(columnValue));
									} else {
										// 框架尝试转换
										columnGenerated = new DateColumn(
												new StringColumn(columnValue)
														.asDate());
									}
								}
							} catch (Exception e) {
								throw new IllegalArgumentException(String.format(
										"类型转换错误, 无法将[%s] 转换为[%s]", columnValue,
										"DATE"));
							}
							break;
						default:
							String errorMessage = String.format(
									"您配置的列类型暂不支持 : [%s]", columnType);
							LOG.error(errorMessage);
							throw DataXException
									.asDataXException(
											UnstructuredStorageReaderErrorCode.NOT_SUPPORT_TYPE,
											errorMessage);
					}

					record.addColumn(columnGenerated);

				}
				recordSender.sendToWriter(record);
			} catch (IllegalArgumentException iae) {
				taskPluginCollector
						.collectDirtyRecord(record, iae.getMessage());
			} catch (IndexOutOfBoundsException ioe) {
				taskPluginCollector
						.collectDirtyRecord(record, ioe.getMessage());
			} catch (Exception e) {
				if (e instanceof DataXException) {
					throw (DataXException) e;
				}
				// 每一种转换失败都是脏数据处理,包括数字格式 & 日期格式
				taskPluginCollector.collectDirtyRecord(record, e.getMessage());
			}
		}

		return record;
	}
	public static Record transportOneRecord2(RecordSender recordSender,
											 List<ColumnEntry> columnConfigs, String[] sourceLine,
											 TaskPluginCollector taskPluginCollector) {
		Record record = recordSender.createRecord();
		Column columnGenerated = null;

		// 创建都为String类型column的record
		if (null == columnConfigs || columnConfigs.size() == 0) {
			for (String columnValue : sourceLine) {
				// not equalsIgnoreCase, it's all ok if nullFormat is null
//				if (columnValue.equals(nullFormat)) {
				if (columnValue.equals(null)) {
					columnGenerated = new StringColumn(null);
				} else {
					columnGenerated = new StringColumn(columnValue);
				}
				record.addColumn(columnGenerated);
			}
			recordSender.sendToWriter(record);
		} else {
			try {
				for (ColumnEntry columnConfig : columnConfigs) {
					String columnType = columnConfig.getType();
					Integer columnIndex = columnConfig.getIndex();
					String columnConst = columnConfig.getValue();

//					LOG.error("columnType =========================="+columnType+"\r\n");
//					LOG.error("columnIndex =========================="+columnIndex+"\r\n");
//					LOG.error("columnConst =========================="+columnConst+"\r\n");
					String columnValue = null;

					if (null == columnIndex && null == columnConst) {
						throw DataXException
								.asDataXException(
										UnstructuredStorageReaderErrorCode.NO_INDEX_VALUE,
										"由于您配置了type, 则至少需要配置 index 或 value");
					}

					if (null != columnIndex && null != columnConst) {
						throw DataXException
								.asDataXException(
										UnstructuredStorageReaderErrorCode.MIXED_INDEX_VALUE,
										"您混合配置了index, value, 每一列同时仅能选择其中一种");
					}

					if (null != columnIndex) {
						if (columnIndex >= sourceLine.length) {
							String message = String
									.format("您尝试读取的列越界,源文件该行有 [%s] 列,您尝试读取第 [%s] 列, 数据详情[%s]",
											sourceLine.length, columnIndex + 1,
											StringUtils.join(sourceLine, ","));
							LOG.warn(message);
							throw new IndexOutOfBoundsException(message);
						}

						columnValue = sourceLine[columnIndex];
					} else {
						columnValue = columnConst;
					}
					Type type = Type.valueOf(columnType.toUpperCase());
					// it's all ok if nullFormat is null
//					if (columnValue.equals(nullFormat)) {
					if (columnValue.equals(null)) {
						columnValue = null;
					}
					switch (type) {
						case STRING:
							columnGenerated = new StringColumn(columnValue);
							break;
						case LONG:
							try {
								columnGenerated = new LongColumn(columnValue);
							} catch (Exception e) {
								throw new IllegalArgumentException(String.format(
										"类型转换错误, 无法将[%s] 转换为[%s]", columnValue,
										"LONG"));
							}
							break;
						case DOUBLE:
							try {
								columnGenerated = new DoubleColumn(columnValue);
							} catch (Exception e) {
								throw new IllegalArgumentException(String.format(
										"类型转换错误, 无法将[%s] 转换为[%s]", columnValue,
										"DOUBLE"));
							}
							break;
						case BOOLEAN:
							try {
								columnGenerated = new BoolColumn(columnValue);
							} catch (Exception e) {
								throw new IllegalArgumentException(String.format(
										"类型转换错误, 无法将[%s] 转换为[%s]", columnValue,
										"BOOLEAN"));
							}

							break;
						case DATE:
							try {
								if (columnValue == null) {
									Date date = null;
									columnGenerated = new DateColumn(date);
								} else {
									String formatString = columnConfig.getFormat();
									//if (null != formatString) {
									if (StringUtils.isNotBlank(formatString)) {
										// 用户自己配置的格式转换, 脏数据行为出现变化
										DateFormat format = columnConfig
												.getDateFormat();
										columnGenerated = new DateColumn(
												format.parse(columnValue));
									} else {
										// 框架尝试转换
										columnGenerated = new DateColumn(
												new StringColumn(columnValue)
														.asDate());
									}
								}
							} catch (Exception e) {
								throw new IllegalArgumentException(String.format(
										"类型转换错误, 无法将[%s] 转换为[%s]", columnValue,
										"DATE"));
							}
							break;
						default:
							String errorMessage = String.format(
									"您配置的列类型暂不支持 : [%s]", columnType);
							LOG.error(errorMessage);
							throw DataXException
									.asDataXException(
											UnstructuredStorageReaderErrorCode.NOT_SUPPORT_TYPE,
											errorMessage);
					}

					record.addColumn(columnGenerated);

				}
				recordSender.sendToWriter(record);
			} catch (IllegalArgumentException iae) {
				taskPluginCollector
						.collectDirtyRecord(record, iae.getMessage());
			} catch (IndexOutOfBoundsException ioe) {
				taskPluginCollector
						.collectDirtyRecord(record, ioe.getMessage());
			} catch (Exception e) {
				if (e instanceof DataXException) {
					throw (DataXException) e;
				}
				// 每一种转换失败都是脏数据处理,包括数字格式 & 日期格式
				taskPluginCollector.collectDirtyRecord(record, e.getMessage());
			}
		}

		return record;
	}

	public static List<ColumnEntry> getListColumnEntry(
			Configuration configuration, final String path) {
		List<JSONObject> lists = configuration.getList(path, JSONObject.class);
		if (lists == null) {
			return null;
		}
		List<ColumnEntry> result = new ArrayList<ColumnEntry>();
		for (final JSONObject object : lists) {
			result.add(JSON.parseObject(object.toJSONString(),
					ColumnEntry.class));
		}
		return result;
	}

	private enum Type {
		STRING, LONG, BOOLEAN, DOUBLE, DATE,BYTE
	}

	/**
	 * check parameter:encoding, compress, filedDelimiter
	 * */
	public static void validateParameter(Configuration readerConfiguration) {

		// encoding check
		validateEncoding(readerConfiguration);

		//only support compress types
		validateCompress(readerConfiguration);

		//fieldDelimiter check
		validateFieldDelimiter(readerConfiguration);

		// column: 1. index type 2.value type 3.when type is Date, may have format
		validateColumn(readerConfiguration);

	}

	public static void validateEncoding(Configuration readerConfiguration) {
		// encoding check
		String encoding = readerConfiguration
				.getString(
						com.alibaba.datax.plugin.unstructuredstorage.reader.Key.ENCODING,
						com.alibaba.datax.plugin.unstructuredstorage.reader.Constant.DEFAULT_ENCODING);
		try {
			encoding = encoding.trim();
			readerConfiguration.set(Key.ENCODING, encoding);
			Charsets.toCharset(encoding);
		} catch (UnsupportedCharsetException uce) {
			throw DataXException.asDataXException(UnstructuredStorageReaderErrorCode.ILLEGAL_VALUE,
					String.format("不支持您配置的编码格式 : [%s]", encoding), uce);
		} catch (Exception e) {
			throw DataXException.asDataXException(UnstructuredStorageReaderErrorCode.CONFIG_INVALID_EXCEPTION,
					String.format("编码配置异常, 请联系我们: %s", e.getMessage()), e);
		}
	}

	public static void validateCompress(Configuration readerConfiguration) {
		String compress =readerConfiguration
				.getUnnecessaryValue(com.alibaba.datax.plugin.unstructuredstorage.reader.Key.COMPRESS,null,null);
		if(StringUtils.isNotBlank(compress)){
			compress = compress.toLowerCase().trim();
			boolean compressTag = "gzip".equals(compress) || "bzip2".equals(compress) || "zip".equals(compress)
					|| "lzo".equals(compress) || "lzo_deflate".equals(compress) || "hadoop-snappy".equals(compress)
					|| "framing-snappy".equals(compress);
			if (!compressTag) {
				throw DataXException.asDataXException(UnstructuredStorageReaderErrorCode.ILLEGAL_VALUE,
						String.format("仅支持 gzip, bzip2, zip, lzo, lzo_deflate, hadoop-snappy, framing-snappy " +
								"文件压缩格式, 不支持您配置的文件压缩格式: [%s]", compress));
			}
		}else{
			// 用户可能配置的是 compress:"",空字符串,需要将compress设置为null
			compress = null;
		}
		readerConfiguration.set(com.alibaba.datax.plugin.unstructuredstorage.reader.Key.COMPRESS, compress);

	}

	public static void validateFieldDelimiter(Configuration readerConfiguration) {
		//fieldDelimiter check
		String delimiterInStr = readerConfiguration.getString(com.alibaba.datax.plugin.unstructuredstorage.reader.Key.FIELD_DELIMITER,null);
		if(null == delimiterInStr){
			throw DataXException.asDataXException(UnstructuredStorageReaderErrorCode.REQUIRED_VALUE,
					String.format("您提供配置文件有误，[%s]是必填参数.",
							com.alibaba.datax.plugin.unstructuredstorage.reader.Key.FIELD_DELIMITER));
		}else if(1 != delimiterInStr.length()){
			// warn: if have, length must be one
			throw DataXException.asDataXException(UnstructuredStorageReaderErrorCode.ILLEGAL_VALUE,
					String.format("仅仅支持单字符切分, 您配置的切分为 : [%s]", delimiterInStr));
		}
	}

	public static void validateColumn(Configuration readerConfiguration) {
		// column: 1. index type 2.value type 3.when type is Date, may have
		// format
		List<Configuration> columns = readerConfiguration
				.getListConfiguration(com.alibaba.datax.plugin.unstructuredstorage.reader.Key.COLUMN);
		if (null == columns || columns.size() == 0) {
			throw DataXException.asDataXException(UnstructuredStorageReaderErrorCode.REQUIRED_VALUE, "您需要指定 columns");
		}
		// handle ["*"]
		if (null != columns && 1 == columns.size()) {
			String columnsInStr = columns.get(0).toString();
			if ("\"*\"".equals(columnsInStr) || "'*'".equals(columnsInStr)) {
				readerConfiguration.set(com.alibaba.datax.plugin.unstructuredstorage.reader.Key.COLUMN, null);
				columns = null;
			}
		}

		if (null != columns && columns.size() != 0) {
			for (Configuration eachColumnConf : columns) {
				eachColumnConf.getNecessaryValue(com.alibaba.datax.plugin.unstructuredstorage.reader.Key.TYPE,
						UnstructuredStorageReaderErrorCode.REQUIRED_VALUE);
				Integer columnIndex = eachColumnConf
						.getInt(com.alibaba.datax.plugin.unstructuredstorage.reader.Key.INDEX);
				String columnValue = eachColumnConf
						.getString(com.alibaba.datax.plugin.unstructuredstorage.reader.Key.VALUE);

				if (null == columnIndex && null == columnValue) {
					throw DataXException.asDataXException(UnstructuredStorageReaderErrorCode.NO_INDEX_VALUE,
							"由于您配置了type, 则至少需要配置 index 或 value");
				}

				if (null != columnIndex && null != columnValue) {
					throw DataXException.asDataXException(UnstructuredStorageReaderErrorCode.MIXED_INDEX_VALUE,
							"您混合配置了index, value, 每一列同时仅能选择其中一种");
				}
				if (null != columnIndex && columnIndex < 0) {
					throw DataXException.asDataXException(UnstructuredStorageReaderErrorCode.ILLEGAL_VALUE,
							String.format("index需要大于等于0, 您配置的index为[%s]", columnIndex));
				}
			}
		}
	}

	public static void validateCsvReaderConfig(Configuration readerConfiguration) {
		String  csvReaderConfig = readerConfiguration.getString(Key.CSV_READER_CONFIG);
		if(StringUtils.isNotBlank(csvReaderConfig)){
			try{
				UnstructuredStorageReaderUtil.csvReaderConfigMap = JSON.parseObject(csvReaderConfig, new TypeReference<HashMap<String, Object>>() {});
			}catch (Exception e) {
				LOG.info(String.format("WARN!!!!忽略csvReaderConfig配置! 配置错误,值只能为空或者为Map结构,您配置的值为: %s", csvReaderConfig));
			}
		}
	}

	public static  String getTimeNow(){
		Date date =  new Date();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		return sdf.format(date);
	}

	/**
	 *
	 * @Title: getRegexPathParent
	 * @Description: 获取正则表达式目录的父目录
	 * @param @param regexPath
	 * @param @return
	 * @return String
	 * @throws
	 */
	public static String getRegexPathParent(String regexPath){
		int endMark;
		for (endMark = 0; endMark < regexPath.length(); endMark++) {
			if ('*' != regexPath.charAt(endMark) && '?' != regexPath.charAt(endMark)) {
				continue;
			} else {
				break;
			}
		}
		int lastDirSeparator = regexPath.substring(0, endMark).lastIndexOf(IOUtils.DIR_SEPARATOR);
		String parentPath  = regexPath.substring(0,lastDirSeparator + 1);

		return  parentPath;
	}
	/**
	 *
	 * @Title: getRegexPathParentPath
	 * @Description: 获取含有通配符路径的父目录，目前只支持在最后一级目录使用通配符*或者?.
	 * (API jcraft.jsch.ChannelSftp.ls(String path)函数限制)  http://epaul.github.io/jsch-documentation/javadoc/
	 * @param @param regexPath
	 * @param @return
	 * @return String
	 * @throws
	 */
	public static String getRegexPathParentPath(String regexPath){
		int lastDirSeparator = regexPath.lastIndexOf(IOUtils.DIR_SEPARATOR);
		String parentPath = "";
		parentPath = regexPath.substring(0,lastDirSeparator + 1);
		if(parentPath.contains("*") || parentPath.contains("?")){
			throw DataXException.asDataXException(UnstructuredStorageReaderErrorCode.ILLEGAL_VALUE,
					String.format("配置项目path中：[%s]不合法，目前只支持在最后一级目录使用通配符*或者?", regexPath));
		}
		return parentPath;
	}

	public static void setCsvReaderConfig(CsvReader csvReader){
		if(null != UnstructuredStorageReaderUtil.csvReaderConfigMap && !UnstructuredStorageReaderUtil.csvReaderConfigMap.isEmpty()){
			try {
				BeanUtils.populate(csvReader,UnstructuredStorageReaderUtil.csvReaderConfigMap);
				LOG.info(String.format("csvReaderConfig设置成功,设置后CsvReader:%s", JSON.toJSONString(csvReader)));
			} catch (Exception e) {
				LOG.info(String.format("WARN!!!!忽略csvReaderConfig配置!通过BeanUtils.populate配置您的csvReaderConfig发生异常,您配置的值为: %s;请检查您的配置!CsvReader使用默认值[%s]",
						JSON.toJSONString(UnstructuredStorageReaderUtil.csvReaderConfigMap),JSON.toJSONString(csvReader)));
			}
		}else {
			//默认关闭安全模式, 放开10W字节的限制
			csvReader.setSafetySwitch(false);
			LOG.info(String.format("CsvReader使用默认值[%s],csvReaderConfig值为[%s]",JSON.toJSONString(csvReader),JSON.toJSONString(UnstructuredStorageReaderUtil.csvReaderConfigMap)));
		}
	}


	public static JSONObject fileNameFinal(String filePath){
		JSONObject ob = new JSONObject();
		int a = filePath.lastIndexOf("/");
		String sourceFilePath = filePath.substring(0,a+1);
		String fileName = filePath.substring(a+1);
		ob.put("fileName",fileName);
		ob.put("sourceFilePath",sourceFilePath);
		return ob;
	}

	public static Map<String,Object>  getMapByFileNameAndByte(String filePath,byte[] buffer){
		JSONObject ob = fileNameFinal(filePath);
		Map<String,Object> map = new HashMap<>();
		map.put("fileName",ob.get("fileName"));
		map.put("sourceFilePath",ob.get("sourceFilePath"));
		map.put("buffer",buffer);
		return map;
	}


	/**
	 * 写入到服务器本地
	 * @param inputStream
	 */
	public static Map fileReadWrite(InputStream inputStream,String fileName) {

		Map<String,String> map = new HashMap<>();

		String[] fileNames = fileName.split("/");

		String filename = fileNames[fileNames.length-1];

		String filePath = null;

		String uuid = UUID.randomUUID().toString().trim().replaceAll("-", "");

		String path2 = null;//文件夹路径
		boolean bak = false;
		if(isOSLinux()){//
			path2 = "/tmp";
			for(int i=0;i<fileNames.length-1;i++){
				path2 = path2 + "/"+fileNames[i];
			}
			filePath = path2 + "/" + uuid + "-"+ filename;
		}else{
			path2 = "D:\\tmp";
			for(int i=0;i<fileNames.length-1;i++){
				path2 = path2 + "\\"+fileNames[i];
			}
			filePath = path2 + "\\" +  uuid + "-"+ filename;
		}

		File folder = new File(path2);
		if (!folder.exists() && !folder.isDirectory()) {
			boolean mkdir = folder.mkdirs();
			System.out.println(path2+"===================="+mkdir);
			if(mkdir){
				System.out.println("成功创建文件夹:"+"=============="+path2);
				bak = true;
			}else{
				System.out.println("创建文件夹:"+"=============="+path2+"，失败");
			}
		} else {
			System.out.println("路径为："+path2+":文件夹已存在");
			bak = true;
		}

		map.put("filePath",filePath);
		map.put("path2",path2);
		map.put("filename",filename);

		if(bak){
			OutputStream output = null;
			try {
				//创建实例对象
				output = new FileOutputStream(filePath);
				//定义字节数组用于一次性IO流操作的空间大小
				byte[] buffer = new byte[1024];
				//定义int每次接收字节输入流实际每次读取的空间大小长度
				int len = 0;
				//使用循环依次读取buffer空间大小的数据
				//直到read未读取到任何数据返回-1时停止循环,即停止读取
				while((len = inputStream.read(buffer)) != -1){
					//将每次读取到的数据输出到字节输出流中指定的数据对象中
					output.write(buffer,0,len);
				}
			} catch (FileNotFoundException e) {
				throw new RuntimeException(e);
			} catch (IOException e) {
				throw new RuntimeException(e);
			} finally {
				//关闭输入流与输出流对象,释放资源
				if (output != null){
					try {
						output.close();
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
				}
				//这里本来要关闭的，但是现在不行，后面会处理的
				if (inputStream != null){
						/*try {
							//inputStream.close();
						} catch (IOException e) {
							throw new RuntimeException(e);
						}*/
				}
			}
		}
		return map;
	}


	/**
	 * 判断是否是liunx
	 * @return
	 */
	public static boolean isOSLinux() {
		Properties prop = System.getProperties();

		String os = prop.getProperty("os.name");
		return os != null && os.toLowerCase().indexOf("linux") > -1;
	}

	/**
	 * 新建文件夹
	 * @param fileName /srouceA/test/
	 * @return
	 */
	public static Boolean mkdirs(String fileName){
		boolean bak = false;
		String path2 = null;
		if(isOSLinux()){//
			path2 = "/temp"+fileName;
		}else{
			fileName = fileName.replace("/","\\\\");
			path2 = "D:"+fileName;
		}
		File folder = new File(path2);
		if (!folder.exists() && !folder.isDirectory()) {
			boolean mkdir = folder.mkdirs();
			System.out.println(path2+"===================="+mkdir);
			if(mkdir){
				System.out.println("成功创建文件夹:"+"=============="+path2);
				bak = true;
			}else{
				System.out.println("创建文件夹:"+"=============="+path2+"，失败");
				bak = false;
			}
		} else {
			System.out.println("路径为："+path2+":文件夹已存在");
			bak = true;
		}
		return bak;
	}
}
