window.eruptSiteConfig = {
    //erupt 接口地址，在前后端分离时指定
    domain: "",
    //附件地址，一般情况下不需要指定，如果自定义对象存储空间，则需在此指定附件资源访问地址
    fileDomain: "",
    //标题
    title: "机器人文件管理",
    //描述
    desc: "请输入文本",
    //是否展示版权信息
    copyright: false,
    //自定义版权内容，1.12.8及以上版本支持，值可以是方法也可以是字符串,1.12.15及以上版本支持渲染标签
    copyrightTxt: function () {
        return "版权信息xxxx"
    },
    //自定义 UI 主题色，默认为蓝色，1.13.3 及以上版本支持
    theme: {
        primaryColor: '#00b3a0'
    },
    //logo路径
    logoPath: "logo.png",
    //菜单折叠后的logo路径，1.12.21及以上版本支持
    logoFoldPath: null,
    //logo文字
    logoText: "livgo"
};

