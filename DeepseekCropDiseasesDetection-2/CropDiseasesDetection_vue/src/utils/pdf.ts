import html2canvas from 'html2canvas';
import jsPDF from 'jspdf';

// 通过XMLHttpRequest获取图片并转换为base64（更可靠的方式）
function imageToBase64(url: string): Promise<string> {
    return new Promise((resolve) => {
        const xhr = new XMLHttpRequest();
        xhr.onload = function () {
            if (xhr.status === 200) {
                const reader = new FileReader();
                reader.onloadend = () => resolve(reader.result as string);
                reader.onerror = () => resolve('');
                reader.readAsDataURL(xhr.response);
            } else {
                console.warn('图片加载失败:', url, xhr.status);
                resolve('');
            }
        };
        xhr.onerror = function () {
            console.warn('图片加载失败:', url);
            resolve('');
        };
        xhr.open('GET', url);
        xhr.responseType = 'blob';
        xhr.send();
    });
}

export const htmlToPDF = async (htmlId: string, title: string = "报表", bgColor = "#fff") => {
    const originalDom = document.getElementById(htmlId) as HTMLElement;
    if (!originalDom) {
        console.error("未找到指定的 DOM 元素");
        return;
    }

    // 克隆原始 DOM
    const cloneDom = originalDom.cloneNode(true) as HTMLElement;
    cloneDom.style.overflow = 'visible';
    cloneDom.style.height = 'auto';
    cloneDom.style.position = 'absolute';
    cloneDom.style.top = '-9999px';
    cloneDom.style.left = '0';
    cloneDom.style.width = originalDom.scrollWidth + 'px';
    document.body.appendChild(cloneDom);

    // 处理所有图片，将URL转换为base64
    const images = cloneDom.querySelectorAll('img');
    const imagePromises: Promise<void>[] = [];

    images.forEach((img: HTMLImageElement) => {
        const src = img.getAttribute('src');
        if (src && !src.startsWith('data:')) {
            const promise = imageToBase64(src).then(base64 => {
                if (base64) {
                    img.setAttribute('src', base64);
                } else {
                    // 图片加载失败，创建一个占位canvas
                    const canvas = document.createElement('canvas');
                    canvas.width = 200;
                    canvas.height = 150;
                    const ctx = canvas.getContext('2d');
                    if (ctx) {
                        ctx.fillStyle = '#f5f5f5';
                        ctx.fillRect(0, 0, 200, 150);
                        ctx.fillStyle = '#999';
                        ctx.font = '14px Arial';
                        ctx.textAlign = 'center';
                        ctx.fillText('图片加载失败', 100, 80);
                    }
                    img.setAttribute('src', canvas.toDataURL());
                }
            });
            imagePromises.push(promise);
        }
    });

    // 等待所有图片处理完成
    await Promise.all(imagePromises);

    // 额外等待确保DOM更新
    await new Promise(resolve => setTimeout(resolve, 300));

    try {
        const A4Width = 595.28;
        const canvas = await html2canvas(cloneDom, {
            scale: 2,
            useCORS: false,
            allowTaint: true,
            backgroundColor: bgColor,
            scrollY: -window.scrollY,
            logging: false,
            imageTimeout: 60000,
        });

        // 移除克隆的 DOM
        document.body.removeChild(cloneDom);

        const imgWidth = A4Width;
        const imgHeight = (A4Width / canvas.width) * canvas.height;
        const pageData = canvas.toDataURL("image/jpeg", 1.0);

        const PDF = new jsPDF("p", 'pt', 'a4');

        // 如果内容高度超过A4页面高度，需要分页
        const A4Height = 841.89;
        if (imgHeight > A4Height) {
            let heightLeft = imgHeight;
            let position = 0;

            PDF.addImage(pageData, "JPEG", 0, position, imgWidth, imgHeight);
            heightLeft -= A4Height;

            while (heightLeft > 0) {
                position = heightLeft - imgHeight;
                PDF.addPage();
                PDF.addImage(pageData, "JPEG", 0, position, imgWidth, imgHeight);
                heightLeft -= A4Height;
            }
        } else {
            PDF.addImage(pageData, "JPEG", 0, 0, imgWidth, imgHeight);
        }

        PDF.save(title + ".pdf");
    } catch (error) {
        // 确保在出错时也移除克隆的DOM
        if (document.body.contains(cloneDom)) {
            document.body.removeChild(cloneDom);
        }
        console.error("PDF生成失败:", error);
        throw error;
    }
};
