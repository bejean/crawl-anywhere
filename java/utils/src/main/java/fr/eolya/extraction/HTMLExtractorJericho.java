package fr.eolya.extraction;

import it.sauronsoftware.base64.Base64;
import net.htmlparser.jericho.*;

public class HTMLExtractorJericho {

	Source source=null;

	public HTMLExtractorJericho()
	{
	}	

	public boolean parse(String rawData)
	{
		try
		{
			MicrosoftConditionalCommentTagTypes.register();
			PHPTagTypes.register();
			PHPTagTypes.PHP_SHORT.deregister(); // remove PHP short tags for this example otherwise they override processing instructions
			MasonTagTypes.register();
			source=new Source(rawData);

			// Call fullSequentialParse manually as most of the source will be parsed.
			source.fullSequentialParse();
			return true;
		}
		catch (Exception e)
		{
			e.printStackTrace();
			source=null;
		}
		return false;

		/*		
		System.out.println("Document title:");
		String title=getTitle(source);
		System.out.println(title==null ? "(none)" : title);

		System.out.println("\nDocument description:");
		String description=getMetaValue(source,"description");
		System.out.println(description==null ? "(none)" : description);

		System.out.println("\nDocument keywords:");
		String keywords=getMetaValue(source,"keywords");
		System.out.println(keywords==null ? "(none)" : keywords);

		System.out.println("\nLinks to other documents:");
		List<Element> linkElements=source.getAllElements(HTMLElementName.A);
		for (Element linkElement : linkElements) {
			String href=linkElement.getAttributeValue("href");
			if (href==null) continue;
			// A element can contain other tags so need to extract the text from it:
			String label=linkElement.getContent().getTextExtractor().toString();
			System.out.println(label+" <"+href+'>');
		}

		System.out.println("\nAll text from file (exluding content inside SCRIPT and STYLE elements):\n");
		System.out.println(source.getTextExtractor().setIncludeAttributes(true).toString());

		System.out.println("\nSame again but this time extend the TextExtractor class to also exclude text from P elements and any elements with class=\"control\":\n");
		TextExtractor textExtractor=new TextExtractor(source) {
			public boolean excludeElement(StartTag startTag) {
				return startTag.getName()==HTMLElementName.P || "control".equalsIgnoreCase(startTag.getAttributeValue("class"));
			}
		};
		System.out.println(textExtractor.setIncludeAttributes(true).toString());
		 */
	}	

	public String getText()
	{
		if (source==null) return null;
		return source.getTextExtractor().setIncludeAttributes(true).toString();
	}

	public String getTextBase64(String charSet)
	{
		if (source==null) return null;
		return Base64.encode(source.getTextExtractor().setIncludeAttributes(true).toString(), charSet);
	}

	public String getTitle()
	{
		if (source==null) return null;
		Element titleElement=source.getFirstElement(HTMLElementName.TITLE);

		if (titleElement==null || "".equals(CharacterReference.decodeCollapseWhiteSpace(titleElement.getContent()))) {
			titleElement=source.getFirstElement(HTMLElementName.H1);
			if (titleElement==null || "".equals(CharacterReference.decodeCollapseWhiteSpace(titleElement.getContent()))) {
				titleElement=source.getFirstElement(HTMLElementName.H2);
				if (titleElement==null || "".equals(CharacterReference.decodeCollapseWhiteSpace(titleElement.getContent()))) {
					titleElement=source.getFirstElement(HTMLElementName.H3);
				}
			}
			if (titleElement==null) return null;
			// TITLE element never contains other tags so just decode it collapsing whitespace:
			String temp = CharacterReference.decodeCollapseWhiteSpace(titleElement.getContent());
			return temp.replaceAll("\\<.*?\\>", "");
		}
		else {
			// TITLE element never contains other tags so just decode it collapsing whitespace:
			return CharacterReference.decodeCollapseWhiteSpace(titleElement.getContent());
		}
	}

	public String getMeta(String key)
	{
		if (source==null) return null;
		for (int pos=0; pos<source.length();) {
			StartTag startTag=source.getNextStartTag(pos,"name",key,false);
			if (startTag==null) return null;
			if (startTag.getName()==HTMLElementName.META)
				return startTag.getAttributeValue("content"); // Attribute values are automatically decoded
			pos=startTag.getEnd();
		}
		return null;
	}
}
