package cn.mapway.document.test;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import cn.mapway.document.annotation.Doc;

@Doc(value = "ABCD", group = "/泛能云/APP接口/Tst")
@Controller
@RequestMapping(value="doc/123/")
public class TestController {
	
	@Doc(value="Name fetch")
	@RequestMapping("/touch")
	public Ret getname(Req req)
	{
		return null;
	}
}
