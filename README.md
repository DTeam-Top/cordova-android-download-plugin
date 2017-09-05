# cordova-android-download-plugin
实现了apk文件下载以及安装功能

## 安装
```
ionic cordova plugin add https://github.com/DTeam-Top/cordova-android-download-plugin --save
```

## 使用

```
//声明
declare var Downloader: any;

//调用
Downloader.get(params, success, error);
```
- params
    - url       下载链接
    - overwrite 是否覆盖
    - name      名称
    - absPath   路径
    - install   是否自动安装


Example
```
(<any>Downloader).get(
    {
        url: '',
        overwrite: true,
        name: 'temp.apk',
        absPath: '/Download/',
        install: true
    },
    success => console.log("download------>success"),
    err => console.log("download------>error"));
```