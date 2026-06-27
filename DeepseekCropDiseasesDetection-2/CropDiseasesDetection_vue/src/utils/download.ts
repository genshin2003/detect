import request from '/@/utils/request';

/**
 * 通用文件下载函数
 * @param url 请求地址
 * @param filename 下载文件名
 */
export async function downloadFile(url: string, filename: string) {
	try {
		const response = await request.get(url, {
			responseType: 'blob',
		});
		const blob = new Blob([response]);
		const downloadUrl = window.URL.createObjectURL(blob);
		const link = document.createElement('a');
		link.href = downloadUrl;
		link.download = filename;
		document.body.appendChild(link);
		link.click();
		document.body.removeChild(link);
		window.URL.revokeObjectURL(downloadUrl);
	} catch (error) {
		console.error('下载失败:', error);
	}
}

/**
 * 下载PDF报告
 * @param type 类型: img/video/camera
 * @param id 记录ID
 */
export function downloadPdf(type: string, id: number) {
	downloadFile(`/api/export/pdf/${type}/${id}`, `${type}检测报告_${id}.pdf`);
}

/**
 * 下载Excel
 * @param type 类型: img/video/camera
 * @param params 查询参数
 */
export function downloadExcel(type: string, params?: Record<string, string>) {
	const query = params ? '?' + new URLSearchParams(params).toString() : '';
	downloadFile(`/api/export/excel/${type}${query}`, `${type}检测记录.xlsx`);
}
