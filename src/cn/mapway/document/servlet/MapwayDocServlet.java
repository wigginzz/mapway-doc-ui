package cn.mapway.document.servlet;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.nutz.json.Json;
import org.nutz.lang.Files;
import org.nutz.lang.Lang;
import org.nutz.lang.Streams;
import org.nutz.lang.Strings;
import org.nutz.log.Log;
import org.nutz.log.Logs;

import cn.mapway.document.helper.DocHelper;
import cn.mapway.document.helper.JarInfo;
import cn.mapway.document.helper.ParseType;
import cn.mapway.document.module.ApiDoc;
import cn.mapway.document.parser.GenContext;
import cn.mapway.document.resource.Template;

/**
 * 文档服务入口
 * 
 * @author zhangjianshe
 *
 */
public class MapwayDocServlet extends HttpServlet {

	/**
	 * 日志记录器
	 */
	private static Log log = Logs.getLog("Mapway-Document");

	/**
	 * 文档上下文环境
	 */
	private GenContext context;

	/**
	 * 需要扫描的包名称
	 */
	private String packageNames;

	/**
	 * connector 包名
	 */
	private String connectorPackageName;

	/**
	 * connector 类名
	 */
	private String connectorClassName;

	/**
	 * 用于编译的ANT HOME
	 */
	private String antHome;
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Constructor of the object.
	 */
	public MapwayDocServlet() {
		super();
	}

	/**
	 * Destruction of the servlet. <br>
	 */
	public void destroy() {
		super.destroy();

	}

	/**
	 * The doGet method of the servlet. <br>
	 *
	 * This method is called when a form has its tag value method equals to get.
	 * 
	 * @param request
	 *            the request send by the client to the server
	 * @param response
	 *            the response send by the server to the client
	 * @throws ServletException
	 *             if an error occurred
	 * @throws IOException
	 *             if an error occurred
	 */
	public void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		// |-------- Server-----|--app----|servlet| pathinfo
		// PathInfo http://www.ennwifi.cn/mapwaydoc/doc/ demo/123
		String pathInfo = request.getPathInfo();

		dispatch(pathInfo, request, response);

	}

	/**
	 * 调度文档命令
	 * 
	 * @param contype
	 * @param response
	 * @throws IOException
	 */
	private void dispatch(String path, HttpServletRequest request,
			HttpServletResponse response) throws IOException {
		log.info("Path " + path);
		if (Strings.isBlank(path) || path.equals("/") || path.equals("/index")
				|| path.equals("/index.html")) {
			genhtml(request, response);
			return;
		}
		if (path.endsWith("/clear.cache.gif")) {
			byteout(response, DocHelper.getClearGifData(), null);
		} else if (path.equals("/help")) {
			html(response,
					Template.readTemplate("/cn/mapway/document/resource/help.html"));
		}

	}

	private static String getDownloadLocal(HttpServletRequest req) {
		String root = req.getServletContext().getRealPath("/");
		String downloadPath = root + "/download/jar";
		Files.createDirIfNoExists(downloadPath);
		return downloadPath;
	}

	/**
	 * 输出文档HTML页面
	 * 
	 * @param request
	 * @param response
	 */
	private void genhtml(HttpServletRequest request,
			HttpServletResponse response) {

		File htmlFile = getCacheFileName();

		if (htmlFile.exists()) {
			try {
				Streams.write(response.getWriter(), Streams.fileInr(htmlFile));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		DocHelper helper = new DocHelper();
		helper.setAntHome(antHome);

		ApiDoc api = helper.toDoc(ParseType.PT_SPRING, context, packageNames);

		String docroot = getDocRoot(request);

		List<JarInfo> jars = helper.jar(api, docroot, connectorPackageName,
				connectorClassName, helper.getClass().getClassLoader()
						.getResource("").getPath()
						+ "/../lib");

		for (JarInfo jar : jars) {// 设定下载的路径
			String localfile = jar.path + "/" + jar.fileName;
			String target = getDownloadLocal(request) + "/" + jar.fileName;
			try {
				Files.copyFile(new File(localfile), new File(target));
			} catch (IOException e) {
				e.printStackTrace();
			}
			jar.link = getBasePath(request) + "download/jar/" + jar.fileName;
		}
		// 删除临时文件
		String sourcePath = getDownloadLocal(request) + File.separator
				+ "source";
		Files.deleteDir(new File(sourcePath));

		// 设置下载目录
		api.getDownloads().addAll(jars);
		String html = helper.genHTML(api);
		html(response, html);
	}

	private File getCacheFileName() {

		return new File(getTempFolder() + "/html.data");
	}

	private String getDocRoot(HttpServletRequest request) {
		return getDownloadLocal(request);
	}

	private void byteout(HttpServletResponse response, byte[] data,
			String contentType) {
		if (!Strings.isBlank(contentType)) {
			response.setContentType(contentType);

		}
		try {
			response.getOutputStream().write(DocHelper.getClearGifData());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void json(HttpServletResponse response, Object data) {
		out(response, Json.toJson(data), "application/json");
	}

	private void html(HttpServletResponse response, String data) {
		out(response, data, "text/html");
	}

	private void out(HttpServletResponse response, String data,
			String contentType) {
		response.setContentType(contentType);
		response.setCharacterEncoding("UTF-8");
		try {
			Lang.writeAll(response.getWriter(), data);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static String getBasePath(HttpServletRequest request) {
		String path = request.getContextPath();
		String port = request.getServerPort() == 80 ? "" : (":" + request
				.getServerPort());

		String basePath = request.getScheme() + "://" + request.getServerName()
				+ port + path + "/";
		return basePath;
	}

	/**
	 * The doPost method of the servlet. <br>
	 *
	 * This method is called when a form has its tag value method equals to
	 * post.
	 * 
	 * @param request
	 *            the request send by the client to the server
	 * @param response
	 *            the response send by the server to the client
	 * @throws ServletException
	 *             if an error occurred
	 * @throws IOException
	 *             if an error occurred
	 */
	public void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		doGet(request, response);
	}

	/**
	 * 作者
	 */
	public final static String PARAM_AUTHOR = "author";
	/**
	 * 标题
	 */
	public final static String PARAM_TITLE = "title";
	/**
	 * 子标题
	 */
	public final static String PARAM_SUB_TITLE = "subtitle";
	/**
	 * 接口URL基路径
	 */
	public final static String PARAM_BASE_PATH = "basePath";
	/**
	 * 域名
	 */
	public final static String PARAM_DOMAIN = "domain";

	/**
	 * ANT hOME路径
	 */
	public final static String PARAM_ANT_HOME = "antHome";
	/**
	 * 扫描的包名，支持，号隔开多个包名
	 */
	public final static String PARAM_SCAN_PACKAGES = "scanPackages";
	/**
	 * 连接的包名
	 */
	public final static String PARAM_CONNECTOR_PACKAGE_NAME = "connectorPackageName";
	/**
	 * 连接的类名
	 */
	public final static String PARAM_CONNECTOR_CLASS_NAME = "connectorClassName";

	/**
	 * Initialization of the servlet. <br>
	 *
	 * @throws ServletException
	 *             if an error occurs
	 */
	public void init() throws ServletException {
		log.info("文档服务初始化");
		context = new GenContext();
		context.setAuthor(Strings.sBlank(this.getInitParameter(PARAM_AUTHOR)));
		context.setDocTitle(Strings.sBlank(this.getInitParameter(PARAM_TITLE)));
		context.setDomain(Strings.sBlank(this.getInitParameter(PARAM_DOMAIN)));
		context.setBasepath(Strings.sBlank(this
				.getInitParameter(PARAM_BASE_PATH)));
		antHome = Strings.sBlank(getInitParameter(PARAM_ANT_HOME));
		context.setSubtitle(Strings.sBlank(this
				.getInitParameter(PARAM_SUB_TITLE)));
		packageNames = Strings.sBlank(this
				.getInitParameter(PARAM_SCAN_PACKAGES));
		connectorPackageName = Strings.sBlank(this
				.getInitParameter(PARAM_CONNECTOR_PACKAGE_NAME));
		connectorClassName = Strings.sBlank(this
				.getInitParameter(PARAM_CONNECTOR_CLASS_NAME));

		// 删除临时文件
		File htmlCache = getCacheFileName();
		if (htmlCache.exists()) {
			htmlCache.delete();
		}
	}

	public String getTempFolder() {
		String folder = System.getProperty("java.io.tmpdir");
		return folder;
	}
}
