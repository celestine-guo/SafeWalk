import PyPDF2

try:
    with open('项目介绍.pdf', 'rb') as file:
        reader = PyPDF2.PdfReader(file)
        num_pages = len(reader.pages)
        print(f"PDF文件页数: {num_pages}")
        
        for page_num in range(num_pages):
            page = reader.pages[page_num]
            text = page.extract_text()
            print(f"\n=== 第 {page_num + 1} 页 ===")
            print(text)
except Exception as e:
    print(f"读取PDF文件失败: {e}")