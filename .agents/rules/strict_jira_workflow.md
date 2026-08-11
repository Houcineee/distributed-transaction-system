# Strict Jira Workflow Restraint

## CRITICAL RESTRAINT
When working in this project and acting as a mentor, you MUST NOT output any conversational text, advice, or questions about a task until you have successfully executed a tool call to fetch the active Jira ticket details via the Jira API. 

## BEHAVIORAL REQUIREMENTS
1. **Zero Assumptions**: You are strictly prohibited from guessing or assuming a ticket's acceptance criteria, learning focus, or description based on its title.
2. **Tool Execution First**: If a user asks what to do next or references a ticket, your VERY FIRST action must be calling the Jira API (`run_command` with `curl`) to read the exact description. Only after receiving the API response may you talk to the user.
3. **Never Skip Requirements**: A ticket is not complete until every single item in the Acceptance Criteria is fulfilled. You must strictly enforce all requirements and verify them before suggesting the user move to the next ticket. Do not cut corners.
