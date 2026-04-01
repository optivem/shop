package shell

import (
	"encoding/base64"
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"net/url"
	"strings"

	"github.com/optivem/gh-scaffold/internal/log"
)

// SonarCloud wraps SonarCloud API calls.
type SonarCloud struct {
	Token string
	Org   string
}

func NewSonarCloud(token, org string) *SonarCloud {
	return &SonarCloud{Token: token, Org: org}
}

func (s *SonarCloud) api(method, endpoint string, data map[string]string) (map[string]interface{}, error) {
	apiURL := "https://sonarcloud.io/api" + endpoint

	var body io.Reader
	if method == "POST" && data != nil {
		vals := url.Values{}
		for k, v := range data {
			vals.Set(k, v)
		}
		body = strings.NewReader(vals.Encode())
	}

	req, err := http.NewRequest(method, apiURL, body)
	if err != nil {
		return nil, err
	}

	creds := base64.StdEncoding.EncodeToString([]byte(s.Token + ":"))
	req.Header.Set("Authorization", "Basic "+creds)
	if body != nil {
		req.Header.Set("Content-Type", "application/x-www-form-urlencoded")
	}

	resp, err := http.DefaultClient.Do(req)
	if err != nil {
		return nil, err
	}
	defer resp.Body.Close()

	raw, _ := io.ReadAll(resp.Body)
	var result map[string]interface{}
	if len(raw) > 0 {
		json.Unmarshal(raw, &result)
	}
	if result == nil {
		result = make(map[string]interface{})
	}

	if resp.StatusCode >= 400 {
		result["error"] = true
		result["status"] = float64(resp.StatusCode)
		if result["message"] == nil {
			result["message"] = string(raw)
		}
	}

	return result, nil
}

func (s *SonarCloud) isAlreadyExists(result map[string]interface{}) bool {
	if err, ok := result["error"]; !ok || err != true {
		return false
	}
	msg, _ := result["message"].(string)
	return strings.Contains(strings.ToLower(msg), "already exist")
}

func (s *SonarCloud) CreateOrg() {
	result, err := s.api("POST", "/organizations/create", map[string]string{
		"key": s.Org, "name": s.Org,
	})
	if err != nil {
		log.Warnf("SonarCloud org creation failed: %v", err)
		return
	}
	if e, ok := result["error"]; ok && e == true && !s.isAlreadyExists(result) {
		log.Warnf("SonarCloud org creation: %v", result["message"])
	} else {
		log.OKf("SonarCloud org: %s", s.Org)
	}
}

func (s *SonarCloud) CreateProject(key string) {
	result, err := s.api("POST", "/projects/create", map[string]string{
		"organization": s.Org, "project": key, "name": key,
	})
	if err != nil {
		log.Warnf("SonarCloud project %s creation failed: %v", key, err)
		return
	}
	if e, ok := result["error"]; ok && e == true && !s.isAlreadyExists(result) {
		log.Warnf("SonarCloud project %s: %v", key, result["message"])
	} else {
		log.OKf("SonarCloud project: %s", key)
	}

	// Rename default branch master -> main
	result, _ = s.api("POST", "/project_branches/rename", map[string]string{
		"project": key, "name": "main",
	})
	if e, ok := result["error"]; ok && e == true {
		msg := fmt.Sprintf("%v", result["message"])
		if !strings.Contains(strings.ToLower(msg), "already exists") {
			log.Warnf("SonarCloud branch rename for %s: %s", key, msg)
		}
	}
}

func (s *SonarCloud) DeleteProject(key string) {
	s.api("POST", "/projects/delete", map[string]string{"project": key})
	log.OKf("Deleted SonarCloud project: %s", key)
}
