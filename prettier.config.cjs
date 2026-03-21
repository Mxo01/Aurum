/** @type {import("prettier").Config} */
module.exports = {
	plugins: [require("prettier-plugin-java").default],
	useTabs: true,
	tabWidth: 2,
	semi: true,
	singleAttributePerLine: true,
	bracketSameLine: false,
	singleQuote: false,
	printWidth: 100,
	trailingComma: "none",
	arrowParens: "avoid",
	bracketSpacing: true,
	htmlWhitespaceSensitivity: "ignore",
	overrides: [
		{
			files: "*.html",
			options: {
				parser: "angular"
			}
		}
	]
};
