## Go Project Structure

- 我习惯将内部才会用到的模块放到internal包下面，对外提供的（例如：事件）放到api包下面
- 我们每个功能都要有自己单独的包名
- 每一个文件只能存在一个类

## Naming Conventions

- 语言文件我习惯使用小驼峰的方式命名

## Build and Compilation

- 每次改完都要进行编译（gradle clean build），直到通过编译

## Tool Usage

- DeepWiki可以查询到几乎所有需要用到的知识，你需要积极的调用这个工具
- 不要进行任何假设，如果遇到不确定的先使用DeepWiki进行查阅，如果DeepWiki中无法查阅到需要进行询问

## Code Documentation

- 我们总是会编写详细的中文注释，以便后续阅读代码