# Strict Jira Workflow Restraint

## CRITICAL RESTRAINT
When working in this project and acting as a mentor, you MUST NOT output any conversational text, advice, or questions about a task until you have successfully executed a tool call to fetch the active Jira ticket details via the Jira API. 

## BEHAVIORAL REQUIREMENTS
1. **Zero Assumptions**: You are strictly prohibited from guessing or assuming a ticket's acceptance criteria, learning focus, or description based on its title.
2. **Tool Execution First**: If a user asks what to do next or references a ticket, your VERY FIRST action must be calling the Jira API to read the exact description. Only after receiving the API response may you talk to the user.
3. **Single-Call Efficiency (No Redundant Requests)**: Do NOT make multiple exploratory requests or trial-and-error API calls. Atlassian has removed `/rest/api/3/search`; use `POST /rest/api/3/search/jql` directly to fetch active issues (`status = 'In Progress'`) or fetch the specific issue via `GET /rest/api/3/issue/{key}` in a single targeted request.
4. **Never Skip Requirements**: A ticket is not complete until every single item in the Acceptance Criteria is fulfilled. You must strictly enforce all requirements and verify them before suggesting the user move to the next ticket. Do not cut corners.

## STANDARD JIRA API CALL PATTERN
To fetch in-progress tickets in a single shot without redundant calls:
```bash
python3 -c '
import urllib.request, json, base64

with open(".internship/jira-config.json") as f:
    cfg = json.load(f)

auth = base64.b64encode(f"{cfg[\"email\"]}:{cfg[\"api_token\"]}".encode()).decode()
req = urllib.request.Request(
    f"{cfg[\"jira_url\"]}/rest/api/3/search/jql",
    data=json.dumps({"jql": f"project = {cfg[\"project_key\"]} AND status = \"In Progress\"", "fields": ["summary", "status", "issuetype", "description", "labels"]}).encode(),
    headers={"Authorization": f"Basic {auth}", "Content-Type": "application/json", "Accept": "application/json"}
)
with urllib.request.urlopen(req) as r:
    print(r.read().decode())
'
```
