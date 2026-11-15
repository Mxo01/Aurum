import { afterEach, describe, expect, it, vi } from "vitest";
import {
	MESSAGE,
	decimalError,
	decimalText,
	decimalValue,
	isFutureDate,
	parseDecimalInput
} from "./validation";
import type { DecimalRule } from "./validation";

const WHOLE_INPUT = "42";
const WHOLE_VALUE = 42;
const DECIMAL_VALUE = 42.5;
const DOT_DECIMAL_INPUT = "42.5";
const DOT_IN_THREE_TRAILING_DIGITS_INPUT = "1.234";
const DOT_IN_THREE_TRAILING_DIGITS_VALUE = 1.234;
const COMMA_DECIMAL_INPUT = "42,5";
const PADDED_DECIMAL_INPUT = "  42,5  ";
const PLUS_SIGNED_INPUT = "+42.5";
const MINUS_SIGNED_INPUT = "-42.5";
const NEGATIVE_DECIMAL_VALUE = -42.5;
const LEADING_SEPARATOR_INPUT = ".5";
const LEADING_SEPARATOR_VALUE = 0.5;
const SIGNED_LEADING_SEPARATOR_INPUT = "-.5";
const SIGNED_LEADING_SEPARATOR_VALUE = -0.5;
const TRAILING_SEPARATOR_INPUT = "1.";
const TRAILING_SEPARATOR_VALUE = 1;
const EMPTY_INPUT = "";
const BLANK_INPUT = "   ";
const WORD_INPUT = "twelve";
const UNIT_SUFFIX_INPUT = "42 euros";
const DOUBLE_DOT_INPUT = "1.2.3";
const DOUBLE_COMMA_INPUT = "1,2,3";
const INNER_SPACE_INPUT = "4 2";
const DOUBLE_SIGN_INPUT = "--42";
const OVERFLOW_DIGIT_COUNT = 400;
const OVERFLOW_INPUT = "9".repeat(OVERFLOW_DIGIT_COUNT);
const FALLBACK_VALUE = 0;

const MIN_ZERO = 0;
const MIN_ONE = 1;
const BELOW_MIN_INPUT = "-1";
const AT_MIN_INPUT = "0";
const ABOVE_MIN_INPUT = "1";
const AT_MIN_ONE_INPUT = "1";
const CUSTOM_REQUIRED_MESSAGE = "Enter the target amount";
const CUSTOM_INVALID_MESSAGE = "Enter a valid amount";
const CUSTOM_MIN_MESSAGE = "Too small";

const NAME_MAX_LENGTH = 50;
const SHORT_NAME_MAX_LENGTH = 12;

const FIXED_TODAY = "2026-11-15";
const PAST_DAY = "2026-11-14";
const FUTURE_DAY = "2026-11-16";
const FIXED_NOW = new Date(2026, 10, 15, 12, 0);

describe("MESSAGE.nameTooLong", () => {
	it.each([
		[NAME_MAX_LENGTH, "Maximum 50 characters"],
		[SHORT_NAME_MAX_LENGTH, "Maximum 12 characters"]
	])("should name the limit of %i characters", (max, expectedMessage) => {
		// GIVEN
		const testSubject = MESSAGE.nameTooLong;

		// WHEN
		const expected = testSubject(max);

		// THEN
		expect(expected).toBe(expectedMessage);
	});
});

describe("parseDecimalInput", () => {
	it.each<{ case: string; input: string; expected: number | null }>([
		{ case: "a whole number", input: WHOLE_INPUT, expected: WHOLE_VALUE },
		{ case: "a dot decimal", input: DOT_DECIMAL_INPUT, expected: DECIMAL_VALUE },
		{ case: "a comma decimal", input: COMMA_DECIMAL_INPUT, expected: DECIMAL_VALUE },
		{
			case: "a dot before three digits is still a decimal separator",
			input: DOT_IN_THREE_TRAILING_DIGITS_INPUT,
			expected: DOT_IN_THREE_TRAILING_DIGITS_VALUE
		},
		{ case: "surrounding spaces", input: PADDED_DECIMAL_INPUT, expected: DECIMAL_VALUE },
		{ case: "a leading plus sign", input: PLUS_SIGNED_INPUT, expected: DECIMAL_VALUE },
		{ case: "a leading minus sign", input: MINUS_SIGNED_INPUT, expected: NEGATIVE_DECIMAL_VALUE },
		{
			case: "a leading separator",
			input: LEADING_SEPARATOR_INPUT,
			expected: LEADING_SEPARATOR_VALUE
		},
		{
			case: "a sign with a leading separator",
			input: SIGNED_LEADING_SEPARATOR_INPUT,
			expected: SIGNED_LEADING_SEPARATOR_VALUE
		},
		{
			case: "a trailing separator",
			input: TRAILING_SEPARATOR_INPUT,
			expected: TRAILING_SEPARATOR_VALUE
		},
		{ case: "an empty string", input: EMPTY_INPUT, expected: null },
		{ case: "only spaces", input: BLANK_INPUT, expected: null },
		{ case: "a word", input: WORD_INPUT, expected: null },
		{ case: "a trailing unit", input: UNIT_SUFFIX_INPUT, expected: null },
		{ case: "two dot separators", input: DOUBLE_DOT_INPUT, expected: null },
		{ case: "two comma separators", input: DOUBLE_COMMA_INPUT, expected: null },
		{ case: "an inner space", input: INNER_SPACE_INPUT, expected: null },
		{ case: "two signs", input: DOUBLE_SIGN_INPUT, expected: null },
		{ case: "a value beyond the number range", input: OVERFLOW_INPUT, expected: null }
	])("should parse $case", ({ input, expected }) => {
		// GIVEN
		const testSubject = parseDecimalInput;

		// WHEN
		const expectedValue = testSubject(input);

		// THEN
		expect(expectedValue).toBe(expected);
	});
});

describe("decimalError", () => {
	it.each<{ case: string; input: string; rule: DecimalRule | undefined; expected: string | null }>([
		{
			case: "the default sentence for an empty required amount",
			input: EMPTY_INPUT,
			rule: undefined,
			expected: MESSAGE.amountRequired
		},
		{
			case: "the custom sentence for an empty required amount",
			input: EMPTY_INPUT,
			rule: { required: CUSTOM_REQUIRED_MESSAGE },
			expected: CUSTOM_REQUIRED_MESSAGE
		},
		{
			case: "no sentence for an empty optional amount",
			input: BLANK_INPUT,
			rule: { optional: true },
			expected: null
		},
		{
			case: "no sentence for an empty optional amount with a custom required sentence",
			input: EMPTY_INPUT,
			rule: { optional: true, required: CUSTOM_REQUIRED_MESSAGE },
			expected: null
		},
		{
			case: "the default sentence for an unparsable amount",
			input: WORD_INPUT,
			rule: undefined,
			expected: MESSAGE.numberInvalid
		},
		{
			case: "the custom sentence for an unparsable amount",
			input: UNIT_SUFFIX_INPUT,
			rule: { invalid: CUSTOM_INVALID_MESSAGE },
			expected: CUSTOM_INVALID_MESSAGE
		},
		{
			case: "the default sentence below the minimum",
			input: BELOW_MIN_INPUT,
			rule: { min: MIN_ZERO },
			expected: MESSAGE.amountNotNegative
		},
		{
			case: "the custom sentence below the minimum",
			input: BELOW_MIN_INPUT,
			rule: { min: MIN_ZERO, minMessage: CUSTOM_MIN_MESSAGE },
			expected: CUSTOM_MIN_MESSAGE
		},
		{
			case: "no sentence for an amount without a minimum",
			input: WHOLE_INPUT,
			rule: undefined,
			expected: null
		},
		{
			case: "no sentence at an inclusive minimum",
			input: AT_MIN_INPUT,
			rule: { min: MIN_ZERO },
			expected: null
		},
		{
			case: "no sentence above the minimum",
			input: ABOVE_MIN_INPUT,
			rule: { min: MIN_ZERO },
			expected: null
		},
		{
			case: "the default sentence at an exclusive minimum",
			input: AT_MIN_INPUT,
			rule: { min: MIN_ZERO, exclusiveMin: true },
			expected: MESSAGE.amountNotNegative
		},
		{
			case: "the custom sentence at an exclusive minimum",
			input: AT_MIN_ONE_INPUT,
			rule: { min: MIN_ONE, exclusiveMin: true, minMessage: MESSAGE.amountPositive },
			expected: MESSAGE.amountPositive
		},
		{
			case: "no sentence above an exclusive minimum",
			input: ABOVE_MIN_INPUT,
			rule: { min: MIN_ZERO, exclusiveMin: true },
			expected: null
		}
	])("should return $case", ({ input, rule, expected }) => {
		// GIVEN
		const testSubject = decimalError;

		// WHEN
		const expectedMessage = testSubject(input, rule);

		// THEN
		expect(expectedMessage).toBe(expected);
	});
});

describe("decimalText", () => {
	it("should accept a valid decimal", () => {
		// GIVEN
		const testSubject = decimalText();

		// WHEN
		const expected = testSubject.safeParse(COMMA_DECIMAL_INPUT);

		// THEN
		expect(expected.success).toBe(true);
	});

	it("should reject an empty amount with the default sentence", () => {
		// GIVEN
		const testSubject = decimalText();

		// WHEN
		const expected = testSubject.safeParse(EMPTY_INPUT);

		// THEN
		expect(expected.error?.issues[0]?.message).toBe(MESSAGE.amountRequired);
	});

	it("should reject an unparsable amount with the default sentence", () => {
		// GIVEN
		const testSubject = decimalText({ optional: true });

		// WHEN
		const expected = testSubject.safeParse(WORD_INPUT);

		// THEN
		expect(expected.error?.issues[0]?.message).toBe(MESSAGE.numberInvalid);
	});

	it("should reject a value at an exclusive minimum with the configured sentence", () => {
		// GIVEN
		const testSubject = decimalText({
			min: MIN_ZERO,
			exclusiveMin: true,
			minMessage: MESSAGE.amountPositive
		});

		// WHEN
		const expected = testSubject.safeParse(AT_MIN_INPUT);

		// THEN
		expect(expected.error?.issues[0]?.message).toBe(MESSAGE.amountPositive);
	});
});

describe("decimalValue", () => {
	it("should return the parsed amount", () => {
		// GIVEN
		const testSubject = decimalValue;

		// WHEN
		const expected = testSubject(COMMA_DECIMAL_INPUT);

		// THEN
		expect(expected).toBe(DECIMAL_VALUE);
	});

	it("should fall back to zero for an unparsable amount", () => {
		// GIVEN
		const testSubject = decimalValue;

		// WHEN
		const expected = testSubject(WORD_INPUT);

		// THEN
		expect(expected).toBe(FALLBACK_VALUE);
	});
});

describe("isFutureDate", () => {
	afterEach(() => {
		vi.useRealTimers();
	});

	it.each([
		["the next day", FUTURE_DAY, true],
		["today", FIXED_TODAY, false],
		["the previous day", PAST_DAY, false]
	])("should answer %s against today's date", (_case, value, expected) => {
		// GIVEN
		vi.useFakeTimers();
		vi.setSystemTime(FIXED_NOW);
		const testSubject = isFutureDate;

		// WHEN
		const expectedResult = testSubject(value);

		// THEN
		expect(expectedResult).toBe(expected);
	});

	it.each([
		["the next day", FUTURE_DAY, true],
		["the given day", FIXED_TODAY, false],
		["the previous day", PAST_DAY, false]
	])("should answer %s against an explicit today", (_case, value, expected) => {
		// GIVEN
		const testSubject = isFutureDate;

		// WHEN
		const expectedResult = testSubject(value, FIXED_TODAY);

		// THEN
		expect(expectedResult).toBe(expected);
	});
});
