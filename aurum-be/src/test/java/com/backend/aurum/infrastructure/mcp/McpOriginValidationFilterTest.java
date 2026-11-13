package com.backend.aurum.infrastructure.mcp;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class McpOriginValidationFilterTest {

	@Test
	void allowsRequestsWithoutAnOrigin() throws Exception {
		McpOriginValidationFilter testSubject = new McpOriginValidationFilter("");
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/mcp");
		MockHttpServletResponse response = new MockHttpServletResponse();
		MockFilterChain chain = new MockFilterChain();

		testSubject.doFilter(request, response, chain);

		assertThat(chain.getRequest()).isNotNull();
		assertThat(response.getStatus()).isEqualTo(200);
	}

	@Test
	void rejectsABrowserOriginWhenTheAllowlistIsEmpty() throws Exception {
		McpOriginValidationFilter testSubject = new McpOriginValidationFilter("");
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/mcp");
		request.addHeader("Origin", "https://evil.test");
		MockHttpServletResponse response = new MockHttpServletResponse();
		MockFilterChain chain = new MockFilterChain();

		testSubject.doFilter(request, response, chain);

		assertThat(chain.getRequest()).isNull();
		assertThat(response.getStatus()).isEqualTo(403);
		assertThat(response.getContentAsString()).contains("invalid_origin");
	}

	@Test
	void allowsAConfiguredOrigin() throws Exception {
		McpOriginValidationFilter testSubject = new McpOriginValidationFilter(
			"https://claude.ai, https://app.test/"
		);
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/mcp");
		request.addHeader("Origin", "https://app.test");
		MockHttpServletResponse response = new MockHttpServletResponse();
		MockFilterChain chain = new MockFilterChain();

		testSubject.doFilter(request, response, chain);

		assertThat(chain.getRequest()).isNotNull();
	}

	@Test
	void ignoresOtherPaths() throws Exception {
		McpOriginValidationFilter testSubject = new McpOriginValidationFilter("");
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/assets");
		request.addHeader("Origin", "https://evil.test");
		MockHttpServletResponse response = new MockHttpServletResponse();
		MockFilterChain chain = new MockFilterChain();

		testSubject.doFilter(request, response, chain);

		assertThat(chain.getRequest()).isNotNull();
	}
}
